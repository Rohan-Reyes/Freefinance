package com.example.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.os.Build
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Snapshot of the server-sync state, observed by the Settings UI.
 */
data class ServerSyncState(
    val signedIn: Boolean = false,
    val initialized: Boolean = false,
    val serverUrl: String = ServerSyncManager.DEFAULT_SERVER_URL,
    val username: String = "",
    val deviceNo: Long = 0,
    val syncing: Boolean = false,
    val lastSyncAt: Long = 0,
    val lastError: String? = null,
    val pendingChanges: Int = 0
)

/** Result of signing in, used by the UI to decide how the first sync should behave. */
data class ServerSignInInfo(
    val serverRecordCount: Int,
    val localHasData: Boolean
)

class ServerSyncException(message: String, val code: Int = 0) : Exception(message)

/**
 * ServerSyncManager — two-way sync between this device's Room database and the
 * FreeFinance sync server (server/server.js).
 *
 * Design (no changes to Room entities or DAOs are needed):
 *  - SQLite triggers on every synced table write the changed row id into `ff_sync_log`.
 *    Triggers are muted (ff_sync_state.applying = 1) while remote changes are applied.
 *  - IDs: rows created on this device keep normal positive autoincrement ids.
 *    On the server every row has a GLOBAL id = deviceNo * 2^40 + localId.
 *    Rows that come from other devices are stored locally with NEGATIVE ids (-globalId),
 *    so SQLite AUTOINCREMENT on this device is never pushed into another device's range.
 *  - Conflicts: last write wins by timestamp (enforced by the server and on pull).
 *  - Account balances are recalculated (initial_balance + cleared transactions) after
 *    every pull, so concurrent offline edits on several devices can't corrupt balances.
 */
class ServerSyncManager private constructor(context: Context) {

    companion object {
        const val DEFAULT_SERVER_URL = "https://budgets.aten-systems.site"
        private const val DEVICE_SPAN = 1L shl 40
        private const val PREFS_TABLE = "prefs"
        private const val PREFS_ROW_ID = 1L
        private const val PUSH_BATCH = 400
        private const val AUTO_SYNC_INTERVAL_MS = 45_000L
        private const val CHANGE_DEBOUNCE_MS = 4_000L

        /** Synced Room tables and the columns in each that hold row ids of other synced rows. */
        private val SYNCED_TABLES: Map<String, List<String>> = linkedMapOf(
            "categories" to listOf("parent_category_id"),
            "accounts" to listOf("parent_account_id"),
            "freelance_projects" to emptyList(),
            "debts" to emptyList(),
            "assets" to emptyList(),
            "exchange_rates" to emptyList(),
            "payment_cards" to listOf("linked_account_id"),
            "transactions" to listOf("account_id", "to_account_id", "category_id"),
            "budgets" to listOf("category_id"),
            "milestones" to listOf("project_id"),
            "planned_payouts" to listOf("target_account_id", "category_id", "paid_transaction_id"),
            "receipts_inbox" to listOf("converted_transaction_id")
        )

        /** Columns that change as a side effect of other rows and must not be logged as user edits. */
        private val DERIVED_COLUMNS: Map<String, Set<String>> = mapOf(
            "accounts" to setOf("current_balance")
        )

        private const val NOW_MS_SQL = "CAST((julianday('now') - 2440587.5) * 86400000.0 AS INTEGER)"

        @Volatile
        private var INSTANCE: ServerSyncManager? = null

        fun get(context: Context): ServerSyncManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ServerSyncManager(context.applicationContext).also { INSTANCE = it }
            }
    }

    private val appContext = context.applicationContext
    private val database: AppDatabase = AppDatabase.getDatabase(appContext)
    private val userPrefs = UserPreferencesRepository(appContext)
    private val sp: SharedPreferences = appContext.getSharedPreferences("ff_server_sync", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<ServerSyncState> = _state.asStateFlow()

    private var autoSyncJob: Job? = null
    private var debounceJob: Job? = null
    private var observerRegistered = false
    private val changeObserver = object : InvalidationTracker.Observer(SYNCED_TABLES.keys.toTypedArray()) {
        override fun onInvalidated(tables: Set<String>) {
            if (!isReady()) return
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(CHANGE_DEBOUNCE_MS)
                syncNowQuietly()
            }
        }
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /** Signs in (or re-signs in, keeping this device's number). Does not sync yet. */
    suspend fun signIn(serverUrl: String, username: String, password: String): ServerSignInInfo =
        withContext(Dispatchers.IO) {
            val base = normalizeUrl(serverUrl)
            val body = JSONObject().apply {
                put("username", username.trim())
                put("password", password)
                put("deviceName", "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(80))
                put("kind", "android")
                val known = sp.getLong("device_no", 0L)
                if (known > 0) put("deviceNo", known)
            }
            val res = request(base, "POST", "/api/login", body, token = null)
            val deviceNo = res.getLong("deviceNo")
            val previousDevice = sp.getLong("device_no", 0L)
            sp.edit()
                .putString("server_url", base)
                .putString("username", res.optString("username", username.trim()))
                .putString("token", res.getString("token"))
                .putLong("device_no", deviceNo)
                .apply()
            if (previousDevice != 0L && previousDevice != deviceNo) {
                // Server assigned a different device number: local ids can't be trusted; start over.
                sp.edit().putBoolean("initialized", false).putLong("cursor", 0L).apply()
            }
            val me = request(base, "GET", "/api/me", null, res.getString("token"))
            publishState()
            ServerSignInInfo(
                serverRecordCount = me.optInt("liveRecords", 0),
                localHasData = localHasUserData()
            )
        }

    /** First sync when the server is empty (or the user wants to merge): uploads everything on this device. */
    suspend fun beginByUploadingLocal() = withContext(Dispatchers.IO) {
        installTriggers()
        database.runInTransaction(Runnable {
            val db = database.openHelper.writableDatabase
            for (table in existingSyncedTables(db)) {
                db.execSQL("INSERT OR REPLACE INTO ff_sync_log(tbl, row_id, op, ts) SELECT '$table', id, 'U', $NOW_MS_SQL FROM `$table`")
            }
        })
        sp.edit().putBoolean("initialized", true).putLong("cursor", 0L).remove("prefs_synced_currency").apply()
        startObserving()
        syncNow()
    }

    /** First sync when the server already has data: replaces this device's data with the server's. */
    suspend fun beginByReplacingLocal() = withContext(Dispatchers.IO) {
        installTriggers()
        database.runInTransaction(Runnable {
            val db = database.openHelper.writableDatabase
            setApplying(db, true)
            db.execSQL("PRAGMA defer_foreign_keys = ON")
            for (table in existingSyncedTables(db).reversed()) {
                db.execSQL("DELETE FROM `$table`")
            }
            db.execSQL("DELETE FROM ff_sync_log")
            setApplying(db, false)
        })
        // Don't push this device's (default) currency over the server's; the pull will set it.
        sp.edit().putBoolean("initialized", true).putLong("cursor", 0L)
            .putString("prefs_synced_currency", userPrefs.baseCurrency.first()).apply()
        startObserving()
        syncNow()
    }

    /** Signs out: stops syncing and removes triggers. Local data stays on the device. */
    suspend fun signOut() = withContext(Dispatchers.IO) {
        stopAutoSync()
        stopObserving()
        removeTriggers()
        // Keep device_no so signing in again reuses this device's id range.
        sp.edit().remove("token").putBoolean("initialized", false).putLong("cursor", 0L)
            .remove("last_error").apply()
        publishState()
    }

    /** Push local changes, then pull remote ones. Throws on failure. */
    suspend fun syncNow() = withContext(Dispatchers.IO) {
        if (!isReady()) throw ServerSyncException("Not signed in")
        syncMutex.withLock {
            _state.value = _state.value.copy(syncing = true, lastError = null)
            try {
                push()
                pull()
                sp.edit().putLong("last_sync_at", System.currentTimeMillis()).remove("last_error").apply()
            } catch (e: Exception) {
                val msg = e.message ?: e.javaClass.simpleName
                sp.edit().putString("last_error", msg).apply()
                if (e is ServerSyncException && e.code == 401) {
                    sp.edit().remove("token").apply()
                }
                throw e
            } finally {
                publishState()
            }
        }
    }

    /** Fire-and-forget sync used by lifecycle hooks; errors are only recorded in state. */
    fun syncNowQuietly() {
        if (!isReady()) return
        scope.launch {
            try { syncNow() } catch (_: Exception) { /* recorded in state */ }
        }
    }

    /** Call from Activity.onResume. */
    fun onAppForeground() {
        if (!isReady()) return
        scope.launch {
            try { installTriggers() } catch (_: Exception) { }
            startObserving()
        }
        autoSyncJob?.cancel()
        autoSyncJob = scope.launch {
            while (isActive) {
                try { syncNow() } catch (_: Exception) { }
                delay(AUTO_SYNC_INTERVAL_MS)
            }
        }
    }

    /** Call from Activity.onPause. */
    fun onAppBackground() {
        stopAutoSync()
        syncNowQuietly() // flush anything typed just before leaving
    }

    // ------------------------------------------------------------------
    // Push
    // ------------------------------------------------------------------

    private data class LogEntry(val table: String, val rowId: Long, val op: String, val ts: Long)

    private suspend fun push() {
        val base = serverUrl()
        val token = token() ?: throw ServerSyncException("Not signed in", 401)
        pushPrefsIfChanged(base, token)
        var guard = 0
        while (guard++ < 500) {
            val entries = readLog(PUSH_BATCH)
            if (entries.isEmpty()) break
            val changes = JSONArray()
            val db = database.openHelper.readableDatabase
            for (e in entries) {
                val globalId = toGlobal(e.rowId)
                if (globalId <= 0) continue
                val row = if (e.op == "D") null else readRow(db, e.table, e.rowId)
                val change = JSONObject()
                    .put("tbl", e.table)
                    .put("id", globalId)
                    .put("ts", e.ts)
                if (row == null) change.put("deleted", true) else change.put("data", row)
                changes.put(change)
            }
            request(base, "POST", "/api/sync/push", JSONObject().put("changes", changes), token)
            // Remove exactly the entries we sent; a newer edit made meanwhile keeps its log row.
            database.runInTransaction(Runnable {
                val w = database.openHelper.writableDatabase
                for (e in entries) {
                    w.execSQL("DELETE FROM ff_sync_log WHERE tbl = ? AND row_id = ? AND ts = ?", arrayOf<Any?>(e.table, e.rowId, e.ts))
                }
            })
        }
    }

    private fun readLog(limit: Int): List<LogEntry> {
        val out = ArrayList<LogEntry>()
        database.openHelper.readableDatabase
            .query("SELECT tbl, row_id, op, ts FROM ff_sync_log ORDER BY ts LIMIT $limit").use { c ->
                while (c.moveToNext()) out.add(LogEntry(c.getString(0), c.getLong(1), c.getString(2), c.getLong(3)))
            }
        return out
    }

    private fun readRow(db: SupportSQLiteDatabase, table: String, localId: Long): JSONObject? {
        val fkCols = SYNCED_TABLES[table] ?: return null
        db.query("SELECT * FROM `$table` WHERE id = ?", arrayOf<Any?>(localId)).use { c ->
            if (!c.moveToFirst()) return null
            val obj = JSONObject()
            for (i in 0 until c.columnCount) {
                val name = c.getColumnName(i)
                when (c.getType(i)) {
                    Cursor.FIELD_TYPE_NULL -> obj.put(name, JSONObject.NULL)
                    Cursor.FIELD_TYPE_INTEGER -> {
                        val v = c.getLong(i)
                        obj.put(name, if (name == "id" || name in fkCols) toGlobal(v) else v)
                    }
                    Cursor.FIELD_TYPE_FLOAT -> {
                        val d = c.getDouble(i)
                        obj.put(name, if (d.isFinite()) d else JSONObject.NULL)
                    }
                    Cursor.FIELD_TYPE_STRING -> obj.put(name, c.getString(i))
                    else -> { /* BLOBs are not synced */ }
                }
            }
            return obj
        }
    }

    private suspend fun pushPrefsIfChanged(base: String, token: String) {
        val currency = userPrefs.baseCurrency.first()
        if (currency == sp.getString("prefs_synced_currency", null)) return
        val change = JSONObject()
            .put("tbl", PREFS_TABLE).put("id", PREFS_ROW_ID).put("ts", System.currentTimeMillis())
            .put("data", JSONObject().put("base_currency", currency))
        request(base, "POST", "/api/sync/push", JSONObject().put("changes", JSONArray().put(change)), token)
        sp.edit().putString("prefs_synced_currency", currency).apply()
    }

    // ------------------------------------------------------------------
    // Pull
    // ------------------------------------------------------------------

    private suspend fun pull() {
        val base = serverUrl()
        val token = token() ?: throw ServerSyncException("Not signed in", 401)
        var cursor = sp.getLong("cursor", 0L)
        var appliedAny = false
        var guard = 0
        while (guard++ < 1000) {
            val res = request(base, "GET", "/api/sync/pull?since=$cursor", null, token)
            val records = res.getJSONArray("records")
            if (records.length() > 0) {
                val (changed, remoteCurrency) = applyRemote(records)
                if (changed) appliedAny = true
                if (remoteCurrency != null) {
                    userPrefs.setBaseCurrency(remoteCurrency)
                    sp.edit().putString("prefs_synced_currency", remoteCurrency).apply()
                }
            }
            cursor = res.getLong("cursor")
            sp.edit().putLong("cursor", cursor).apply()
            if (!res.optBoolean("hasMore", false)) break
        }
        if (appliedAny) {
            database.accountDao().recalculateAllAccountBalances()
            database.invalidationTracker.refreshVersionsAsync()
        }
    }

    /**
     * Applies a batch of server records in one transaction.
     * Returns (any row changed, base currency from the server if it was in the batch).
     */
    private fun applyRemote(records: JSONArray): Pair<Boolean, String?> {
        var changed = false
        var remoteCurrency: String? = null
        database.runInTransaction(Runnable {
            val db = database.openHelper.writableDatabase
            setApplying(db, true)
            db.execSQL("PRAGMA defer_foreign_keys = ON")
            val columnsCache = HashMap<String, Set<String>>()
            for (i in 0 until records.length()) {
                val r = records.getJSONObject(i)
                val table = r.getString("tbl")
                if (table == PREFS_TABLE) {
                    if (!r.optBoolean("deleted", false)) {
                        remoteCurrency = r.optJSONObject("data")?.optString("base_currency")?.takeIf { it.isNotBlank() }
                    }
                    continue
                }
                val fkCols = SYNCED_TABLES[table] ?: continue
                val columns = columnsCache.getOrPut(table) { tableColumns(db, table) }
                if (columns.isEmpty()) continue
                val localId = toLocal(r.getLong("id"))
                val remoteTs = r.getLong("ts")

                // Local edit that is newer than the server's copy wins; it will be pushed next.
                val pendingTs = pendingLogTs(db, table, localId)
                if (pendingTs != null && pendingTs > remoteTs) continue
                if (pendingTs != null) {
                    db.execSQL("DELETE FROM ff_sync_log WHERE tbl = ? AND row_id = ?", arrayOf<Any?>(table, localId))
                }

                // One malformed row must never block the whole sync: skip it and keep going.
                try {
                    if (r.optBoolean("deleted", false)) {
                        db.execSQL("DELETE FROM `$table` WHERE id = ?", arrayOf<Any?>(localId))
                    } else {
                        val data = r.optJSONObject("data") ?: continue
                        upsertRow(db, table, columns, fkCols, localId, data)
                    }
                    changed = true
                } catch (e: Exception) {
                    android.util.Log.w("ServerSync", "Skipped $table/$localId: ${e.message}")
                }
            }
            repairForeignKeys(db)
            setApplying(db, false)
        })
        return Pair(changed, remoteCurrency)
    }

    private fun upsertRow(
        db: SupportSQLiteDatabase,
        table: String,
        columns: Set<String>,
        fkCols: List<String>,
        localId: Long,
        data: JSONObject
    ) {
        val cols = ArrayList<String>()
        val values = ArrayList<Any?>()
        val keys = data.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key == "id" || key !in columns) continue
            var v: Any? = data.opt(key)
            if (v == JSONObject.NULL) v = null
            if (key in fkCols && v is Number) v = toLocal(v.toLong())
            cols.add(key)
            values.add(v)
        }
        if (cols.isNotEmpty()) {
            val set = cols.joinToString(", ") { "`$it` = ?" }
            val upd = db.compileStatement("UPDATE `$table` SET $set WHERE id = ?")
            bindAll(upd, values + localId)
            val n = upd.executeUpdateDelete()
            upd.close()
            if (n > 0) return
        }
        val insertCols = listOf("id") + cols
        val placeholders = insertCols.joinToString(", ") { "?" }
        // exchange_rates has a UNIQUE(from, to) index; let the incoming row replace a local duplicate.
        val verb = if (table == "exchange_rates") "INSERT OR REPLACE" else "INSERT"
        val ins = db.compileStatement(
            "$verb INTO `$table` (${insertCols.joinToString(", ") { "`$it`" }}) VALUES ($placeholders)"
        )
        bindAll(ins, listOf<Any?>(localId) + values)
        ins.executeInsert()
        ins.close()
    }

    private fun bindAll(stmt: SupportSQLiteStatement, values: List<Any?>) {
        values.forEachIndexed { idx, v ->
            val i = idx + 1
            when (v) {
                null -> stmt.bindNull(i)
                is Boolean -> stmt.bindLong(i, if (v) 1L else 0L)
                is Int -> stmt.bindLong(i, v.toLong())
                is Long -> stmt.bindLong(i, v)
                is Float -> stmt.bindDouble(i, v.toDouble())
                is Double -> stmt.bindDouble(i, v)
                is Number -> stmt.bindDouble(i, v.toDouble())
                else -> stmt.bindString(i, v.toString()) // String, JSONArray, JSONObject
            }
        }
    }

    /**
     * A remote row can reference a parent this device doesn't have (e.g. deleted elsewhere).
     * Rather than failing the whole batch: nullable references are cleared, rows whose
     * required parent is missing are dropped.
     */
    private fun repairForeignKeys(db: SupportSQLiteDatabase) {
        data class Violation(val table: String, val rowId: Long, val fkId: Int)
        repeat(3) {
            val violations = ArrayList<Violation>()
            db.query("PRAGMA foreign_key_check").use { c ->
                while (c.moveToNext()) {
                    if (!c.isNull(1)) violations.add(Violation(c.getString(0), c.getLong(1), c.getInt(3)))
                }
            }
            if (violations.isEmpty()) return
            for (v in violations) {
                var fromCol: String? = null
                db.query("PRAGMA foreign_key_list(`${v.table}`)").use { c ->
                    val idIdx = c.getColumnIndex("id")
                    val fromIdx = c.getColumnIndex("from")
                    while (c.moveToNext()) if (c.getInt(idIdx) == v.fkId) fromCol = c.getString(fromIdx)
                }
                val col = fromCol
                if (col != null && columnIsNullable(db, v.table, col)) {
                    db.execSQL("UPDATE `${v.table}` SET `$col` = NULL WHERE rowid = ?", arrayOf<Any?>(v.rowId))
                } else {
                    db.execSQL("DELETE FROM `${v.table}` WHERE rowid = ?", arrayOf<Any?>(v.rowId))
                }
            }
        }
    }

    private fun columnIsNullable(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
        db.query("PRAGMA table_info(`$table`)").use { c ->
            val nameIdx = c.getColumnIndex("name")
            val notNullIdx = c.getColumnIndex("notnull")
            while (c.moveToNext()) if (c.getString(nameIdx) == column) return c.getInt(notNullIdx) == 0
        }
        return false
    }

    private fun pendingLogTs(db: SupportSQLiteDatabase, table: String, localId: Long): Long? {
        db.query("SELECT ts FROM ff_sync_log WHERE tbl = ? AND row_id = ?", arrayOf<Any?>(table, localId)).use { c ->
            return if (c.moveToFirst()) c.getLong(0) else null
        }
    }

    // ------------------------------------------------------------------
    // Schema: log table + triggers
    // ------------------------------------------------------------------

    private fun ensureSyncTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS ff_sync_log (" +
                "tbl TEXT NOT NULL, row_id INTEGER NOT NULL, op TEXT NOT NULL, ts INTEGER NOT NULL, " +
                "PRIMARY KEY (tbl, row_id))"
        )
        db.execSQL("CREATE TABLE IF NOT EXISTS ff_sync_state (k INTEGER PRIMARY KEY, applying INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("INSERT OR IGNORE INTO ff_sync_state (k, applying) VALUES (1, 0)")
        db.execSQL("UPDATE ff_sync_state SET applying = 0 WHERE k = 1")
    }

    /** (Re)creates triggers from the live schema, so new columns in future app versions are tracked. */
    private fun installTriggers() {
        database.runInTransaction(Runnable {
            val db = database.openHelper.writableDatabase
            ensureSyncTables(db)
            val muted = "(SELECT applying FROM ff_sync_state WHERE k = 1) = 0"
            for (table in existingSyncedTables(db)) {
                dropTriggers(db, table)
                val log = { idExpr: String, op: String ->
                    "INSERT OR REPLACE INTO ff_sync_log (tbl, row_id, op, ts) VALUES ('$table', $idExpr, '$op', $NOW_MS_SQL);"
                }
                db.execSQL("CREATE TRIGGER ff_ai_$table AFTER INSERT ON `$table` WHEN $muted BEGIN ${log("NEW.id", "U")} END")
                val derived = DERIVED_COLUMNS[table].orEmpty()
                val tracked = tableColumns(db, table).filter { it !in derived }
                val ofCols = if (derived.isEmpty()) "" else " OF " + tracked.joinToString(", ") { "`$it`" }
                db.execSQL("CREATE TRIGGER ff_au_$table AFTER UPDATE$ofCols ON `$table` WHEN $muted BEGIN ${log("NEW.id", "U")} END")
                db.execSQL("CREATE TRIGGER ff_ad_$table AFTER DELETE ON `$table` WHEN $muted BEGIN ${log("OLD.id", "D")} END")
            }
        })
    }

    private fun removeTriggers() {
        database.runInTransaction(Runnable {
            val db = database.openHelper.writableDatabase
            for (table in SYNCED_TABLES.keys) dropTriggers(db, table)
            db.execSQL("DROP TABLE IF EXISTS ff_sync_log")
        })
    }

    private fun dropTriggers(db: SupportSQLiteDatabase, table: String) {
        db.execSQL("DROP TRIGGER IF EXISTS ff_ai_$table")
        db.execSQL("DROP TRIGGER IF EXISTS ff_au_$table")
        db.execSQL("DROP TRIGGER IF EXISTS ff_ad_$table")
    }

    private fun setApplying(db: SupportSQLiteDatabase, applying: Boolean) {
        ensureSyncTables(db)
        db.execSQL("UPDATE ff_sync_state SET applying = ? WHERE k = 1", arrayOf<Any?>(if (applying) 1 else 0))
    }

    private fun existingSyncedTables(db: SupportSQLiteDatabase): List<String> {
        val existing = HashSet<String>()
        db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { c ->
            while (c.moveToNext()) existing.add(c.getString(0))
        }
        return SYNCED_TABLES.keys.filter { it in existing }
    }

    private fun tableColumns(db: SupportSQLiteDatabase, table: String): Set<String> {
        val cols = LinkedHashSet<String>()
        db.query("PRAGMA table_info(`$table`)").use { c ->
            val nameIdx = c.getColumnIndex("name")
            while (c.moveToNext()) cols.add(c.getString(nameIdx))
        }
        return cols
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun deviceNo(): Long = sp.getLong("device_no", 0L)

    /** Local row id -> global id used on the server. */
    private fun toGlobal(localId: Long): Long = when {
        localId > 0 -> deviceNo() * DEVICE_SPAN + localId
        localId < 0 -> -localId
        else -> 0L
    }

    /** Global id from the server -> local row id on this device. */
    private fun toLocal(globalId: Long): Long = when {
        globalId <= 0 -> globalId
        globalId / DEVICE_SPAN == deviceNo() -> globalId - deviceNo() * DEVICE_SPAN
        else -> -globalId
    }

    private fun localHasUserData(): Boolean {
        val db = database.openHelper.readableDatabase
        val tables = existingSyncedTables(db).filter { it != "categories" && it != "exchange_rates" }
        for (t in tables) {
            db.query("SELECT 1 FROM `$t` LIMIT 1").use { c -> if (c.moveToFirst()) return true }
        }
        return false
    }

    private fun isReady(): Boolean = token() != null && sp.getBoolean("initialized", false)
    private fun token(): String? = sp.getString("token", null)
    private fun serverUrl(): String = sp.getString("server_url", DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL

    private fun normalizeUrl(raw: String): String {
        var u = raw.trim().trimEnd('/')
        if (u.isEmpty()) u = DEFAULT_SERVER_URL
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://$u"
        return u
    }

    private fun request(base: String, method: String, path: String, body: JSONObject?, token: String?): JSONObject {
        val builder = Request.Builder().url(base + path)
        if (token != null) builder.header("Authorization", "Bearer $token")
        when (method) {
            "GET" -> builder.get()
            else -> builder.method(method, (body ?: JSONObject()).toString().toRequestBody(jsonMedia))
        }
        try {
            http.newCall(builder.build()).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                val json = try { JSONObject(text) } catch (_: Exception) { JSONObject() }
                if (!resp.isSuccessful) {
                    val msg = json.optString("error").ifBlank { "Server error (${resp.code})" }
                    throw ServerSyncException(msg, resp.code)
                }
                return json
            }
        } catch (e: IOException) {
            throw ServerSyncException("Can't reach server: ${e.message ?: "network error"}")
        }
    }

    private fun startObserving() {
        if (observerRegistered) return
        try {
            database.invalidationTracker.addObserver(changeObserver)
            observerRegistered = true
        } catch (_: Exception) { }
    }

    private fun stopObserving() {
        if (!observerRegistered) return
        try { database.invalidationTracker.removeObserver(changeObserver) } catch (_: Exception) { }
        observerRegistered = false
    }

    private fun stopAutoSync() {
        autoSyncJob?.cancel()
        autoSyncJob = null
    }

    private fun countPending(): Int = try {
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM ff_sync_log").use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    } catch (_: Exception) { 0 }

    private fun readState() = ServerSyncState(
        signedIn = sp.getString("token", null) != null,
        initialized = sp.getBoolean("initialized", false),
        serverUrl = sp.getString("server_url", DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL,
        username = sp.getString("username", "") ?: "",
        deviceNo = sp.getLong("device_no", 0L),
        lastSyncAt = sp.getLong("last_sync_at", 0L),
        lastError = sp.getString("last_error", null)
    )

    private fun publishState() {
        _state.value = readState().copy(syncing = false, pendingChanges = countPending())
    }
}
