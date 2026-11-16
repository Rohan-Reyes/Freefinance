package com.example.data.sync

import android.content.Context
import android.util.Base64
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.ExchangeRateEntity
import com.example.data.local.entity.FreelanceProjectEntity
import com.example.data.local.entity.MilestoneEntity
import com.example.data.local.entity.PaymentCardEntity
import com.example.data.local.entity.PlannedPayoutEntity
import com.example.data.local.entity.ReceiptEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.model.CompoundingFrequency
import com.example.data.local.model.DashboardCardType
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.AppThemePreset
import com.example.data.preferences.HeroBackgroundStyle
import com.example.data.preferences.UserPreferencesRepository
import com.example.util.ReceiptStorageHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

/** Optional text field: null for a missing key, JSON null, or blank (optString would return "null"). */
private fun JSONObject.optText(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key, "").takeIf { it.isNotBlank() }

/**
 * BackupDataDTO: Holds collections of every entity in the app for universal state import/export.
 */
data class BackupDataDTO(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val baseCurrency: String = "USD",
    val accounts: List<AccountEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val freelanceProjects: List<FreelanceProjectEntity> = emptyList(),
    val milestones: List<MilestoneEntity> = emptyList(),
    val exchangeRates: List<ExchangeRateEntity> = emptyList(),
    val paymentCards: List<PaymentCardEntity> = emptyList(),
    val assets: List<AssetEntity> = emptyList(),
    val plannedPayouts: List<PlannedPayoutEntity> = emptyList(),
    val receipts: List<ReceiptEntity> = emptyList(),
    /** Table keys present in the file. Tables a (v1) backup doesn't contain are left untouched on import. */
    val presentTables: Set<String> = emptySet()
)

/**
 * DataSyncManager: Provides robust JSON serialization and deserialization for the entire
 * Room database state, with atomic transaction handling, foreign-key safe table clearing,
 * and OnConflictStrategy.REPLACE to prevent crashes.
 *
 * Backup format v2 (a superset of v1, which still imports):
 *  - every Room table, including assets, plannedPayouts and receiptsInbox;
 *  - "settings": base currency, theme, hero style, dashboard layout, privacy toggles
 *    (app-lock PIN / biometric settings are deliberately NOT exported);
 *  - "images": receipt and proof photos as { "<sha256>": "<base64>" }; rows point at them
 *    with proofSha256 / imageSha256.
 * Import also accepts the web app's export (snake_case fields, global sync ids).
 * [context] is needed for settings and photos; without it only the records are handled.
 */
class DataSyncManager(private val database: AppDatabase, private val context: Context? = null) {

    private companion object {
        const val BACKUP_VERSION = 2
        const val DEVICE_SPAN = 1L shl 40
        const val MAX_PHOTO_BYTES = 15L * 1024 * 1024
        val SHA256_RE = Regex("^[a-f0-9]{64}$")

        /** Web export / sync table names -> keys used in this backup format. */
        val TABLE_ALIASES = mapOf(
            "freelance_projects" to "freelanceProjects",
            "exchange_rates" to "exchangeRates",
            "payment_cards" to "paymentCards",
            "planned_payouts" to "plannedPayouts",
            "receipts_inbox" to "receiptsInbox"
        )
        val TABLE_KEYS = listOf(
            "categories", "accounts", "transactions", "budgets", "debts", "freelanceProjects", "milestones",
            "exchangeRates", "paymentCards", "assets", "plannedPayouts", "receiptsInbox"
        )
        /** Fields holding ids of other rows (camelCase). */
        val REF_FIELDS = setOf(
            "parentCategoryId", "parentAccountId", "linkedAccountId", "accountId", "toAccountId", "categoryId",
            "projectId", "targetAccountId", "paidTransactionId", "convertedTransactionId"
        )
        /** Booleans the web stores as 0/1. */
        val BOOL_FIELDS = setOf("isLocked", "includeInTally", "isVirtual", "isFrozen", "includeInNetWorth")
    }

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun parseDate(value: Any?): Date {
        if (value == null || value == JSONObject.NULL) return Date()
        return when (value) {
            is Number -> Date(value.toLong())
            is String -> {
                try {
                    isoDateFormat.parse(value) ?: Date()
                } catch (e: Exception) {
                    try {
                        simpleDateFormat.parse(value) ?: Date()
                    } catch (e2: Exception) {
                        try {
                            Date(value.toLong())
                        } catch (e3: Exception) {
                            Date()
                        }
                    }
                }
            }
            else -> Date()
        }
    }

    private fun parseNullableDate(value: Any?): Date? {
        if (value == null || value == JSONObject.NULL) return null
        return parseDate(value)
    }

    /**
     * Serializes entire local Room database into a single standard JSON payload.
     */
    suspend fun exportStateToJson(baseCurrency: String = "USD", includePhotos: Boolean = true): String {
        val accounts = database.accountDao().getAllAccounts().first()
        val categories = database.categoryDao().getAllCategories().first()
        val transactions = database.transactionDao().getAllTransactions().first()
        val budgets = database.budgetDao().getAllBudgets().first()
        val debts = database.debtDao().getAllDebts().first()
        val projects = database.freelanceDao().getAllProjects().first()
        val milestones = database.freelanceDao().getAllMilestones().first()
        val exchangeRates = database.exchangeRateDao().getAllRates().first()
        val paymentCards = database.paymentCardDao().getAllCards().first()
        val assets = database.assetDao().getAllAssets().first()
        val plannedPayouts = database.plannedPayoutDao().getAllPlannedPayouts().first()
        val receipts = database.receiptDao().getAllReceipts().first()

        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("baseCurrency", baseCurrency)
        root.put("settings", exportSettings(baseCurrency))

        // Photos are embedded once per distinct file content and referenced by SHA-256.
        val images = JSONObject()
        fun attachPhoto(path: String?, obj: JSONObject, hashKey: String) {
            if (!includePhotos || path.isNullOrBlank()) return
            val bytes = readPhoto(path) ?: return
            val sha = sha256Hex(bytes)
            obj.put(hashKey, sha)
            if (!images.has(sha)) images.put(sha, Base64.encodeToString(bytes, Base64.NO_WRAP))
        }

        // 1. Categories
        val catArray = JSONArray()
        categories.forEach { cat ->
            val obj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("color", cat.color)
                put("iconResId", cat.iconResId)
                put("type", cat.type)
                if (cat.parentCategoryId != null) put("parentCategoryId", cat.parentCategoryId)
            }
            catArray.put(obj)
        }
        root.put("categories", catArray)

        // 2. Accounts
        val accArray = JSONArray()
        accounts.forEach { acc ->
            val obj = JSONObject().apply {
                put("id", acc.id)
                put("name", acc.name)
                put("type", acc.type)
                put("initialBalance", acc.initialBalance)
                put("currentBalance", acc.currentBalance)
                put("color", acc.color)
                put("interestRatePa", acc.interestRatePa.toDouble())
                put("compoundingFrequency", acc.compoundingFrequency.name)
                put("currencyCode", acc.currencyCode)
                put("isLocked", acc.isLocked)
                put("includeInTally", acc.includeInTally)
                if (acc.parentAccountId != null) {
                    put("parentAccountId", acc.parentAccountId)
                }
                if (acc.subAccountType != null) {
                    put("subAccountType", acc.subAccountType)
                }
                if (acc.linkedAppPackage != null) {
                    put("linkedAppPackage", acc.linkedAppPackage)
                }
                if (acc.lockedUntil != null) {
                    put("lockedUntil", isoDateFormat.format(acc.lockedUntil))
                }
            }
            accArray.put(obj)
        }
        root.put("accounts", accArray)

        // 3. Transactions
        val txArray = JSONArray()
        transactions.forEach { tx ->
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("accountId", tx.accountId)
                if (tx.toAccountId != null) put("toAccountId", tx.toAccountId)
                put("type", tx.type)
                put("amount", tx.amount)
                if (tx.categoryId != null) put("categoryId", tx.categoryId)
                put("date", isoDateFormat.format(tx.date))
                put("status", tx.status)
                put("recurrenceRule", tx.recurrenceRule)
                val labelsArr = JSONArray()
                tx.labels.forEach { labelsArr.put(it) }
                put("labels", labelsArr)
                if (tx.note != null) put("note", tx.note)
                if (tx.proofImageUri != null) put("proofImageUri", tx.proofImageUri)
            }
            attachPhoto(tx.proofImageUri, obj, "proofSha256")
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        // 4. Budgets
        val budgetArray = JSONArray()
        budgets.forEach { b ->
            val obj = JSONObject().apply {
                put("id", b.id)
                put("categoryId", b.categoryId)
                put("amountLimit", b.amountLimit)
                put("period", b.period)
                put("startDate", isoDateFormat.format(b.startDate))
                put("endDate", isoDateFormat.format(b.endDate))
            }
            budgetArray.put(obj)
        }
        root.put("budgets", budgetArray)

        // 5. Debts
        val debtArray = JSONArray()
        debts.forEach { d ->
            val obj = JSONObject().apply {
                put("id", d.id)
                put("personName", d.personName)
                put("type", d.type)
                put("totalAmount", d.totalAmount)
                put("remainingAmount", d.remainingAmount)
                if (d.dueDate != null) put("dueDate", isoDateFormat.format(d.dueDate))
                put("status", d.status)
                if (d.notes != null) put("notes", d.notes)
                put("createdAt", isoDateFormat.format(d.createdAt))
            }
            debtArray.put(obj)
        }
        root.put("debts", debtArray)

        // 6. Freelance Projects & Milestones
        val projArray = JSONArray()
        projects.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("projectName", p.projectName)
                put("client", p.client)
                put("totalExpectedFee", p.totalExpectedFee)
                put("status", p.status)
                put("createdAt", isoDateFormat.format(p.createdAt))
            }
            projArray.put(obj)
        }
        root.put("freelanceProjects", projArray)

        val milestoneArray = JSONArray()
        milestones.forEach { m ->
            val obj = JSONObject().apply {
                put("id", m.id)
                put("projectId", m.projectId)
                put("title", m.title)
                put("amount", m.amount)
                put("expectedDate", isoDateFormat.format(m.expectedDate))
                put("status", m.status)
                if (m.paidAt != null) put("paidAt", isoDateFormat.format(m.paidAt))
            }
            milestoneArray.put(obj)
        }
        root.put("milestones", milestoneArray)

        // 7. Exchange Rates
        val rateArray = JSONArray()
        exchangeRates.forEach { r ->
            val obj = JSONObject().apply {
                put("id", r.id)
                put("fromCurrency", r.fromCurrency)
                put("toCurrency", r.toCurrency)
                put("rate", r.rate)
                put("lastUpdated", r.lastUpdated)
            }
            rateArray.put(obj)
        }
        root.put("exchangeRates", rateArray)

        // 8. Payment Cards
        val cardsArray = JSONArray()
        paymentCards.forEach { c ->
            val obj = JSONObject().apply {
                put("id", c.id)
                put("cardName", c.cardName)
                put("cardType", c.cardType)
                put("cardNetwork", c.cardNetwork)
                put("issuingBank", c.issuingBank)
                put("cardholderName", c.cardholderName)
                put("cardNumber", c.cardNumber)
                put("expiryMonth", c.expiryMonth)
                put("expiryYear", c.expiryYear)
                put("cvv", c.cvv)
                if (c.cardPinHint != null) put("cardPinHint", c.cardPinHint)
                if (c.billingAddress != null) put("billingAddress", c.billingAddress)
                if (c.customerServicePhone != null) put("customerServicePhone", c.customerServicePhone)
                put("currencyCode", c.currencyCode)
                put("color", c.color)
                put("cardFinish", c.cardFinish)
                put("isVirtual", c.isVirtual)
                put("isFrozen", c.isFrozen)
                if (c.linkedAccountId != null) put("linkedAccountId", c.linkedAccountId)
                put("dailySpendLimit", c.dailySpendLimit)
                put("creditLimit", c.creditLimit)
                put("currentCreditUsed", c.currentCreditUsed)
                put("statementBalance", c.statementBalance)
                put("minimumPaymentDue", c.minimumPaymentDue)
                if (c.statementDay != null) put("statementDay", c.statementDay)
                if (c.paymentDueDay != null) put("paymentDueDay", c.paymentDueDay)
                put("aprInterestRate", c.aprInterestRate.toDouble())
                put("annualFee", c.annualFee)
                if (c.rewardsProgram != null) put("rewardsProgram", c.rewardsProgram)
                if (c.notes != null) put("notes", c.notes)
                put("createdAt", c.createdAt)
            }
            cardsArray.put(obj)
        }
        root.put("paymentCards", cardsArray)

        // 9. Assets
        val assetArray = JSONArray()
        assets.forEach { a ->
            assetArray.put(JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("category", a.category)
                put("estimatedValue", a.estimatedValue)
                put("purchasePrice", a.purchasePrice)
                if (a.purchaseDate != null) put("purchaseDate", isoDateFormat.format(a.purchaseDate))
                put("currencyCode", a.currencyCode)
                if (a.notes != null) put("notes", a.notes)
                put("iconName", a.iconName)
                put("color", a.color)
                put("includeInNetWorth", a.includeInNetWorth)
                put("createdAt", a.createdAt)
            })
        }
        root.put("assets", assetArray)

        // 10. Planned payouts
        val payoutArray = JSONArray()
        plannedPayouts.forEach { p ->
            payoutArray.put(JSONObject().apply {
                put("id", p.id)
                put("title", p.title)
                put("amount", p.amount)
                put("currencyCode", p.currencyCode)
                put("dueDate", isoDateFormat.format(p.dueDate))
                if (p.targetAccountId != null) put("targetAccountId", p.targetAccountId)
                if (p.categoryId != null) put("categoryId", p.categoryId)
                if (p.payeeOrVendor != null) put("payeeOrVendor", p.payeeOrVendor)
                if (p.notes != null) put("notes", p.notes)
                put("priority", p.priority)
                put("status", p.status)
                if (p.paidDate != null) put("paidDate", isoDateFormat.format(p.paidDate))
                if (p.paidTransactionId != null) put("paidTransactionId", p.paidTransactionId)
                put("createdAt", isoDateFormat.format(p.createdAt))
            })
        }
        root.put("plannedPayouts", payoutArray)

        // 11. Receipt inbox
        val receiptArray = JSONArray()
        receipts.forEach { r ->
            val obj = JSONObject().apply {
                put("id", r.id)
                put("imagePath", r.imagePath)
                put("capturedAt", isoDateFormat.format(r.capturedAt))
                if (r.note != null) put("note", r.note)
                if (r.suggestedAmount != null) put("suggestedAmount", r.suggestedAmount)
                put("suggestedType", r.suggestedType)
                put("status", r.status)
                if (r.convertedTransactionId != null) put("convertedTransactionId", r.convertedTransactionId)
            }
            attachPhoto(r.imagePath, obj, "imageSha256")
            receiptArray.put(obj)
        }
        root.put("receiptsInbox", receiptArray)

        // 12. Photos
        root.put("images", images)

        return root.toString(2)
    }

    /** Display/sync settings. Security settings (app-lock PIN, biometrics) are never exported. */
    private suspend fun exportSettings(baseCurrency: String): JSONObject {
        val out = JSONObject().put("base_currency", baseCurrency)
        val ctx = context ?: return out
        val prefs = UserPreferencesRepository(ctx)
        out.put("app_theme_preset", prefs.appThemePreset.first().name)
        out.put("app_theme_mode", prefs.appThemeMode.first().name)
        out.put("hero_background_style", prefs.heroBackgroundStyle.first().name)
        out.put("include_assets_in_hero", prefs.includeAssetsInHero.first())
        out.put("hide_money", prefs.hideMoney.first())
        out.put("active_dashboard_cards", JSONArray(prefs.activeDashboardCards.first().map { it.name }))
        out.put("dashboard_card_order", prefs.dashboardCardOrder.first().joinToString(",") { it.name })
        return out
    }

    private fun readPhoto(path: String): ByteArray? = try {
        if (path.startsWith("content://")) {
            context?.contentResolver?.openInputStream(android.net.Uri.parse(path))?.use { input ->
                val bytes = input.readBytes()
                if (bytes.size > MAX_PHOTO_BYTES) null else bytes
            }
        } else {
            val file = File(path)
            if (file.isFile && file.length() in 1..MAX_PHOTO_BYTES) file.readBytes() else null
        }
    } catch (_: Exception) {
        null // a missing/unreadable photo never blocks the backup
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    // ---------------------------------------------------------------- import helpers

    private fun camelCase(key: String): String {
        if (!key.contains('_')) return key
        val parts = key.split('_').filter { it.isNotEmpty() }
        if (parts.isEmpty()) return key
        return parts.first() + parts.drop(1).joinToString("") { p -> p.replaceFirstChar { it.uppercaseChar() } }
    }

    /**
     * Makes web exports look like this app's format: table keys, camelCase fields, 0/1 booleans,
     * labels as arrays, and global sync ids (deviceNo * 2^40 + localId) turned into local ids the
     * same way server sync does (this device's rows -> positive local id, others -> -globalId).
     */
    private fun normalizeBackup(root: JSONObject): Boolean {
        var fromWeb = false
        for ((alias, key) in TABLE_ALIASES) {
            if (!root.has(key) && root.has(alias)) { root.put(key, root.get(alias)); fromWeb = true }
        }
        val deviceNo = context?.getSharedPreferences("ff_server_sync", Context.MODE_PRIVATE)?.getLong("device_no", 0L) ?: 0L
        fun mapId(v: Long): Long = when {
            v < DEVICE_SPAN -> v
            deviceNo > 0 && v / DEVICE_SPAN == deviceNo -> v - deviceNo * DEVICE_SPAN
            else -> -v
        }
        for (key in TABLE_KEYS) {
            val arr = root.optJSONArray(key) ?: continue
            val out = JSONArray()
            for (i in 0 until arr.length()) {
                val src = arr.optJSONObject(i) ?: continue
                val obj = JSONObject()
                val keys = src.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val ck = camelCase(k)
                    if (k != ck) fromWeb = true
                    if (k == ck || !obj.has(ck)) obj.put(ck, src.get(k))
                }
                for (f in BOOL_FIELDS) {
                    val v = obj.opt(f)
                    if (v is Number) obj.put(f, v.toInt() != 0)
                }
                val labels = obj.opt("labels")
                if (labels is String) {
                    obj.put("labels", try { JSONArray(labels) } catch (_: Exception) {
                        JSONArray(labels.split(',').map { it.trim() }.filter { it.isNotEmpty() })
                    })
                }
                for (f in REF_FIELDS + "id") {
                    val v = obj.opt(f)
                    if (v is Number) obj.put(f, mapId(v.toLong()))
                }
                out.put(obj)
            }
            root.put(key, out)
        }
        return fromWeb
    }

    /** Writes the backup's photos into the receipts folder and points rows at them. Returns photos restored. */
    private fun restorePhotos(root: JSONObject): Int {
        val images = root.optJSONObject("images") ?: return 0
        val ctx = context ?: return 0
        val dir = ReceiptStorageHelper.getReceiptsDir(ctx)
        val written = HashMap<String, String>()
        val shas = images.keys()
        while (shas.hasNext()) {
            val sha = shas.next()
            if (!SHA256_RE.matches(sha)) continue
            try {
                // Same file name the server sync uses, so a synced copy of the photo is reused.
                val target = File(dir, "SYNC_$sha.jpg")
                if (!target.isFile) {
                    val bytes = Base64.decode(images.getString(sha), Base64.DEFAULT)
                    if (sha256Hex(bytes) != sha) continue
                    val tmp = File(dir, "SYNC_$sha.part")
                    tmp.writeBytes(bytes)
                    if (!tmp.renameTo(target)) { tmp.delete(); continue }
                }
                written[sha] = target.absolutePath
            } catch (_: Exception) {
                // skip a corrupted photo, keep the record
            }
        }
        fun repoint(tableKey: String, hashKey: String, pathKey: String) {
            val arr = root.optJSONArray(tableKey) ?: return
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                written[obj.optString(hashKey, "")]?.let { obj.put(pathKey, it) }
            }
        }
        repoint("transactions", "proofSha256", "proofImageUri")
        repoint("receiptsInbox", "imageSha256", "imagePath")
        return written.size
    }

    /** Restores display settings (v2 backups); a v1 backup only carries the base currency. */
    private suspend fun restoreSettings(root: JSONObject) {
        val ctx = context ?: return
        val prefs = UserPreferencesRepository(ctx)
        val s = root.optJSONObject("settings") ?: JSONObject()
        fun str(k: String): String? = if (s.has(k) && !s.isNull(k)) s.optString(k, "").takeIf { it.isNotBlank() } else null
        fun bool(k: String): Boolean? = when (val v = s.opt(k)) {
            is Boolean -> v
            is Number -> v.toInt() != 0
            else -> null
        }
        (str("base_currency") ?: root.optString("baseCurrency", "").takeIf { it.isNotBlank() })?.let { prefs.setBaseCurrency(it) }
        str("app_theme_preset")?.let { v -> AppThemePreset.entries.find { it.name == v }?.let { prefs.setAppThemePreset(it) } }
        str("app_theme_mode")?.let { v -> AppThemeMode.entries.find { it.name == v.uppercase() }?.let { prefs.setAppThemeMode(it) } }
        str("hero_background_style")?.let { v -> HeroBackgroundStyle.entries.find { it.name == v }?.let { prefs.setHeroBackgroundStyle(it) } }
        bool("include_assets_in_hero")?.let { prefs.setIncludeAssetsInHero(it) }
        bool("hide_money")?.let { prefs.setHideMoney(it) }
        s.optJSONArray("active_dashboard_cards")?.let { arr ->
            val cards = (0 until arr.length()).mapNotNull { DashboardCardType.fromString(arr.optString(it)) }.toSet()
            if (cards.isNotEmpty()) prefs.setActiveCards(cards)
        }
        str("dashboard_card_order")?.let { order ->
            val list = order.split(',').mapNotNull { DashboardCardType.fromString(it.trim()) }
            if (list.isNotEmpty()) prefs.setCardOrder(list)
        }
    }

    /**
     * Parses the given JSON string into [BackupDataDTO] safely with comprehensive try-catch.
     */
    fun parseJsonToDTO(jsonString: String): BackupDataDTO {
        val root = JSONObject(jsonString)
        normalizeBackup(root)
        return parseRoot(root)
    }

    private fun parseRoot(root: JSONObject): BackupDataDTO {
        val version = root.optInt("version", 1)
        val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
        val baseCurrency = root.optString("baseCurrency", "USD")

        // 1. Categories
        val categories = mutableListOf<CategoryEntity>()
        val catArray = root.optJSONArray("categories")
        if (catArray != null) {
            for (i in 0 until catArray.length()) {
                try {
                    val obj = catArray.getJSONObject(i)
                    categories.add(
                        CategoryEntity(
                            id = obj.optLong("id", 0),
                            name = obj.optString("name", "Category"),
                            iconResId = obj.optString("iconResId", "ic_category"),
                            type = obj.optString("type", "Expense"),
                            color = obj.optInt("color", 0xFF2196F3.toInt()),
                            parentCategoryId = if (obj.has("parentCategoryId") && !obj.isNull("parentCategoryId")) {
                                obj.getLong("parentCategoryId")
                            } else null
                        )
                    )
                } catch (e: Exception) {
                    // skip malformed item
                }
            }
        }

        // 2. Accounts
        val accounts = mutableListOf<AccountEntity>()
        val accArray = root.optJSONArray("accounts")
        if (accArray != null) {
            for (i in 0 until accArray.length()) {
                try {
                    val obj = accArray.getJSONObject(i)
                    val compStr = obj.optString("compoundingFrequency", "NONE")
                    val compFreq = try {
                        CompoundingFrequency.valueOf(compStr.uppercase())
                    } catch (e: Exception) {
                        CompoundingFrequency.NONE
                    }
                    val isLocked = obj.optBoolean("isLocked", false) || obj.has("lockedUntil")
                    val lockedUntilDate = parseNullableDate(obj.opt("lockedUntil"))
                    val includeInTally = obj.optBoolean("includeInTally", true)
                    val linkedAppPackage = obj.optText("linkedAppPackage")
                    val parentAccountId = if (obj.has("parentAccountId") && !obj.isNull("parentAccountId")) {
                        obj.getLong("parentAccountId")
                    } else null
                    val subAccountType = obj.optText("subAccountType")
                    accounts.add(
                        AccountEntity(
                            id = obj.optLong("id", 0),
                            name = obj.optString("name", "Account"),
                            type = obj.optString("type", "Bank"),
                            initialBalance = obj.optDouble("initialBalance", 0.0),
                            currentBalance = obj.optDouble("currentBalance", obj.optDouble("initialBalance", 0.0)),
                            color = obj.optInt("color", 0xFF4CAF50.toInt()),
                            interestRatePa = obj.optDouble("interestRatePa", 0.0).toFloat(),
                            compoundingFrequency = compFreq,
                            currencyCode = obj.optString("currencyCode", "USD").uppercase(),
                            isLocked = isLocked,
                            lockedUntil = lockedUntilDate,
                            linkedAppPackage = linkedAppPackage,
                            includeInTally = includeInTally,
                            parentAccountId = parentAccountId,
                            subAccountType = subAccountType
                        )
                    )
                } catch (e: Exception) {
                    // skip malformed item
                }
            }
        }

        // 3. Transactions
        val transactions = mutableListOf<TransactionEntity>()
        val txArray = root.optJSONArray("transactions")
        if (txArray != null) {
            for (i in 0 until txArray.length()) {
                try {
                    val obj = txArray.getJSONObject(i)
                    val labelsList = mutableListOf<String>()
                    val labelsArr = obj.optJSONArray("labels")
                    if (labelsArr != null) {
                        for (j in 0 until labelsArr.length()) {
                            labelsList.add(labelsArr.getString(j))
                        }
                    }
                    transactions.add(
                        TransactionEntity(
                            id = obj.optLong("id", 0),
                            accountId = obj.optLong("accountId", 1),
                            toAccountId = if (obj.has("toAccountId") && !obj.isNull("toAccountId")) obj.getLong("toAccountId") else null,
                            type = obj.optString("type", "Expense"),
                            amount = obj.optDouble("amount", 0.0),
                            categoryId = if (obj.has("categoryId") && !obj.isNull("categoryId")) obj.getLong("categoryId") else null,
                            date = parseDate(obj.opt("date")),
                            status = obj.optString("status", "Cleared"),
                            recurrenceRule = obj.optString("recurrenceRule", "None"),
                            labels = labelsList,
                            note = if (obj.has("note") && !obj.isNull("note")) obj.getString("note") else null,
                            proofImageUri = if (obj.has("proofImageUri") && !obj.isNull("proofImageUri")) obj.getString("proofImageUri") else null
                        )
                    )
                } catch (e: Exception) {
                    // skip malformed item
                }
            }
        }

        // 4. Budgets
        val budgets = mutableListOf<BudgetEntity>()
        val budgetArray = root.optJSONArray("budgets")
        if (budgetArray != null) {
            for (i in 0 until budgetArray.length()) {
                try {
                    val obj = budgetArray.getJSONObject(i)
                    budgets.add(
                        BudgetEntity(
                            id = obj.optLong("id", 0),
                            categoryId = obj.optLong("categoryId", 1),
                            amountLimit = obj.optDouble("amountLimit", 0.0),
                            period = obj.optString("period", "Monthly"),
                            startDate = parseDate(obj.opt("startDate")),
                            endDate = parseDate(obj.opt("endDate"))
                        )
                    )
                } catch (e: Exception) {
                    // skip malformed item
                }
            }
        }

        // 5. Debts
        val debts = mutableListOf<DebtEntity>()
        val debtArray = root.optJSONArray("debts")
        if (debtArray != null) {
            for (i in 0 until debtArray.length()) {
                try {
                    val obj = debtArray.getJSONObject(i)
                    debts.add(
                        DebtEntity(
                            id = obj.optLong("id", 0),
                            personName = obj.optString("personName", "Unnamed"),
                            type = obj.optString("type", "Lent"),
                            totalAmount = obj.optDouble("totalAmount", 0.0),
                            remainingAmount = obj.optDouble("remainingAmount", obj.optDouble("totalAmount", 0.0)),
                            dueDate = parseNullableDate(obj.opt("dueDate")),
                            status = obj.optString("status", "Active"),
                            notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes") else null,
                            createdAt = parseDate(obj.opt("createdAt"))
                        )
                    )
                } catch (e: Exception) {
                    // skip malformed item
                }
            }
        }

        // 6. Freelance Projects & Milestones
        val projects = mutableListOf<FreelanceProjectEntity>()
        val projectArray = root.optJSONArray("freelanceProjects")
        if (projectArray != null) {
            for (i in 0 until projectArray.length()) {
                try {
                    val obj = projectArray.getJSONObject(i)
                    projects.add(
                        FreelanceProjectEntity(
                            id = obj.optLong("id", 0),
                            projectName = obj.optString("projectName", "Project"),
                            client = obj.optString("client", "Client"),
                            totalExpectedFee = obj.optDouble("totalExpectedFee", 0.0),
                            status = obj.optString("status", "Active"),
                            createdAt = parseDate(obj.opt("createdAt"))
                        )
                    )
                } catch (e: Exception) {
                    // skip malformed item
                }
            }
        }

        val milestones = mutableListOf<MilestoneEntity>()
        val milestoneArray = root.optJSONArray("milestones")
        if (milestoneArray != null) {
            for (i in 0 until milestoneArray.length()) {
                try {
                    val obj = milestoneArray.getJSONObject(i)
                    milestones.add(
                        MilestoneEntity(
                            id = obj.optLong("id", 0),
                            projectId = obj.optLong("projectId", 1),
                            title = obj.optString("title", "Milestone"),
                            amount = obj.optDouble("amount", 0.0),
                            expectedDate = parseDate(obj.opt("expectedDate")),
                            status = obj.optString("status", "Pending"),
                            paidAt = parseNullableDate(obj.opt("paidAt"))
                        )
                    )
                } catch (e: Exception) {
                    // skip malformed item
                }
            }
        }

        // 7. Exchange Rates
        val exchangeRates = mutableListOf<ExchangeRateEntity>()
        val rateArray = root.optJSONArray("exchangeRates")
        if (rateArray != null) {
            for (i in 0 until rateArray.length()) {
                try {
                    val obj = rateArray.getJSONObject(i)
                    exchangeRates.add(
                        ExchangeRateEntity(
                            id = obj.optLong("id", 0),
                            fromCurrency = obj.optString("fromCurrency", "USD").uppercase(),
                            toCurrency = obj.optString("toCurrency", "USD").uppercase(),
                            rate = obj.optDouble("rate", 1.0),
                            lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())
                        )
                    )
                } catch (e: Exception) {
                    // skip malformed item
                }
            }
        }

        // 8. Payment Cards
        val paymentCards = mutableListOf<PaymentCardEntity>()
        val cardsArray = root.optJSONArray("paymentCards")
        if (cardsArray != null) {
            for (i in 0 until cardsArray.length()) {
                try {
                    val obj = cardsArray.getJSONObject(i)
                    paymentCards.add(
                        PaymentCardEntity(
                            id = obj.optLong("id", 0),
                            cardName = obj.optString("cardName", "Card"),
                            cardType = obj.optString("cardType", "DEBIT"),
                            cardNetwork = obj.optString("cardNetwork", "VISA"),
                            issuingBank = obj.optString("issuingBank", ""),
                            cardholderName = obj.optString("cardholderName", ""),
                            cardNumber = obj.optString("cardNumber", ""),
                            expiryMonth = obj.optInt("expiryMonth", 12),
                            expiryYear = obj.optInt("expiryYear", 28),
                            cvv = obj.optString("cvv", ""),
                            cardPinHint = obj.optText("cardPinHint"),
                            billingAddress = obj.optText("billingAddress"),
                            customerServicePhone = obj.optText("customerServicePhone"),
                            currencyCode = obj.optString("currencyCode", "PHP"),
                            color = obj.optInt("color", -14579213),
                            cardFinish = obj.optString("cardFinish", "OBSIDIAN_GOLD"),
                            isVirtual = obj.optBoolean("isVirtual", false),
                            isFrozen = obj.optBoolean("isFrozen", false),
                            linkedAccountId = if (obj.has("linkedAccountId") && !obj.isNull("linkedAccountId")) obj.getLong("linkedAccountId") else null,
                            dailySpendLimit = obj.optDouble("dailySpendLimit", 0.0),
                            creditLimit = obj.optDouble("creditLimit", 0.0),
                            currentCreditUsed = obj.optDouble("currentCreditUsed", 0.0),
                            statementBalance = obj.optDouble("statementBalance", 0.0),
                            minimumPaymentDue = obj.optDouble("minimumPaymentDue", 0.0),
                            statementDay = if (obj.has("statementDay") && !obj.isNull("statementDay")) obj.getInt("statementDay") else null,
                            paymentDueDay = if (obj.has("paymentDueDay") && !obj.isNull("paymentDueDay")) obj.getInt("paymentDueDay") else null,
                            aprInterestRate = obj.optDouble("aprInterestRate", 0.0).toFloat(),
                            annualFee = obj.optDouble("annualFee", 0.0),
                            rewardsProgram = obj.optText("rewardsProgram"),
                            notes = obj.optText("notes"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                } catch (_: Exception) {
                }
            }
        }

        // 9. Assets
        val assets = mutableListOf<AssetEntity>()
        root.optJSONArray("assets")?.let { arr ->
            for (i in 0 until arr.length()) {
                try {
                    val obj = arr.getJSONObject(i)
                    assets.add(
                        AssetEntity(
                            id = obj.optLong("id", 0),
                            name = obj.optString("name", "Asset"),
                            category = obj.optString("category", "Other"),
                            estimatedValue = obj.optDouble("estimatedValue", 0.0),
                            purchasePrice = obj.optDouble("purchasePrice", 0.0),
                            purchaseDate = parseNullableDate(obj.opt("purchaseDate")),
                            currencyCode = obj.optString("currencyCode", "USD").uppercase(),
                            notes = obj.optText("notes"),
                            iconName = obj.optString("iconName", "ic_asset"),
                            color = obj.optInt("color", -16738120),
                            includeInNetWorth = obj.optBoolean("includeInNetWorth", true),
                            createdAt = parseDate(obj.opt("createdAt")).time
                        )
                    )
                } catch (_: Exception) {
                    // skip malformed item
                }
            }
        }

        // 10. Planned payouts
        val plannedPayouts = mutableListOf<PlannedPayoutEntity>()
        root.optJSONArray("plannedPayouts")?.let { arr ->
            for (i in 0 until arr.length()) {
                try {
                    val obj = arr.getJSONObject(i)
                    plannedPayouts.add(
                        PlannedPayoutEntity(
                            id = obj.optLong("id", 0),
                            title = obj.optString("title", "Payout"),
                            amount = obj.optDouble("amount", 0.0),
                            currencyCode = obj.optString("currencyCode", "PHP").uppercase(),
                            dueDate = parseDate(obj.opt("dueDate")),
                            targetAccountId = if (obj.has("targetAccountId") && !obj.isNull("targetAccountId")) obj.getLong("targetAccountId") else null,
                            categoryId = if (obj.has("categoryId") && !obj.isNull("categoryId")) obj.getLong("categoryId") else null,
                            payeeOrVendor = obj.optText("payeeOrVendor"),
                            notes = obj.optText("notes"),
                            priority = obj.optString("priority", "NORMAL"),
                            status = obj.optString("status", "PENDING"),
                            paidDate = parseNullableDate(obj.opt("paidDate")),
                            paidTransactionId = if (obj.has("paidTransactionId") && !obj.isNull("paidTransactionId")) obj.getLong("paidTransactionId") else null,
                            createdAt = parseDate(obj.opt("createdAt"))
                        )
                    )
                } catch (_: Exception) {
                    // skip malformed item
                }
            }
        }

        // 11. Receipt inbox
        val receipts = mutableListOf<ReceiptEntity>()
        root.optJSONArray("receiptsInbox")?.let { arr ->
            for (i in 0 until arr.length()) {
                try {
                    val obj = arr.getJSONObject(i)
                    receipts.add(
                        ReceiptEntity(
                            id = obj.optLong("id", 0),
                            imagePath = if (obj.isNull("imagePath")) "" else obj.optString("imagePath", ""),
                            capturedAt = parseDate(obj.opt("capturedAt")),
                            note = obj.optText("note"),
                            suggestedAmount = if (obj.has("suggestedAmount") && !obj.isNull("suggestedAmount")) obj.getDouble("suggestedAmount") else null,
                            suggestedType = obj.optString("suggestedType", "Expense"),
                            status = obj.optString("status", "PENDING"),
                            convertedTransactionId = if (obj.has("convertedTransactionId") && !obj.isNull("convertedTransactionId")) obj.getLong("convertedTransactionId") else null
                        )
                    )
                } catch (_: Exception) {
                    // skip malformed item
                }
            }
        }

        return BackupDataDTO(
            version = version,
            exportedAt = exportedAt,
            baseCurrency = baseCurrency,
            accounts = accounts,
            transactions = transactions,
            categories = categories,
            budgets = budgets,
            debts = debts,
            freelanceProjects = projects,
            milestones = milestones,
            exchangeRates = exchangeRates,
            paymentCards = paymentCards,
            assets = assets,
            plannedPayouts = plannedPayouts,
            receipts = receipts,
            presentTables = TABLE_KEYS.filter { root.optJSONArray(it) != null }.toSet()
        )
    }

    /**
     * Imports the entire application state from a JSON string inside an atomic Room transaction.
     * Disables foreign key constraints temporarily, clears tables cleanly, and inserts all entities.
     * Returns the total count of inserted entities or a failure Result.
     */
    suspend fun importStateFromJson(jsonString: String): Result<Int> {
        return try {
            if (jsonString.isBlank()) {
                return Result.failure(IllegalArgumentException("Import JSON cannot be empty"))
            }

            val root = JSONObject(jsonString)
            val fromWeb = normalizeBackup(root)
            restorePhotos(root) // files first, outside the DB transaction
            val dto = parseRoot(root)

            database.withTransaction {
                val db = database.openHelper.writableDatabase
                // Temporarily disable foreign key checks to avoid constraint exceptions during mass wipe & batch load
                db.execSQL("PRAGMA foreign_keys = OFF")

                try {
                    // Delete all table contents
                    db.execSQL("DELETE FROM transactions")
                    db.execSQL("DELETE FROM milestones")
                    db.execSQL("DELETE FROM budgets")
                    db.execSQL("DELETE FROM freelance_projects")
                    db.execSQL("DELETE FROM debts")
                    db.execSQL("DELETE FROM exchange_rates")
                    db.execSQL("DELETE FROM payment_cards")
                    db.execSQL("DELETE FROM accounts")
                    db.execSQL("DELETE FROM categories")
                    // v2 tables: only replaced when the backup contains them (v1 files leave them as they are)
                    if ("assets" in dto.presentTables) db.execSQL("DELETE FROM assets")
                    if ("plannedPayouts" in dto.presentTables) db.execSQL("DELETE FROM planned_payouts")
                    if ("receiptsInbox" in dto.presentTables) db.execSQL("DELETE FROM receipts_inbox")

                    // Insert parent entities first, followed by dependent children
                    if (dto.categories.isNotEmpty()) {
                        database.categoryDao().insertCategories(dto.categories)
                    }

                    if (dto.accounts.isNotEmpty()) {
                        database.accountDao().insertAccounts(dto.accounts)
                    }

                    if (dto.paymentCards.isNotEmpty()) {
                        database.paymentCardDao().insertCards(dto.paymentCards)
                    }

                    if (dto.exchangeRates.isNotEmpty()) {
                        database.exchangeRateDao().insertRates(dto.exchangeRates)
                    }

                    if (dto.freelanceProjects.isNotEmpty()) {
                        database.freelanceDao().insertProjects(dto.freelanceProjects)
                    }

                    if (dto.milestones.isNotEmpty()) {
                        database.freelanceDao().insertMilestones(dto.milestones)
                    }

                    if (dto.budgets.isNotEmpty()) {
                        database.budgetDao().insertBudgets(dto.budgets)
                    }

                    if (dto.debts.isNotEmpty()) {
                        database.debtDao().insertDebts(dto.debts)
                    }

                    if (dto.transactions.isNotEmpty()) {
                        database.transactionDao().insertTransactions(dto.transactions)
                    }

                    dto.assets.forEach { database.assetDao().insertAsset(it) }
                    dto.plannedPayouts.forEach { database.plannedPayoutDao().insert(it) }
                    dto.receipts.forEach { database.receiptDao().insertReceipt(it) }
                } finally {
                    // Always re-enable foreign keys
                    db.execSQL("PRAGMA foreign_keys = ON")
                }
            }

            // The web app derives balances on the fly, so its stored current_balance can be stale.
            if (fromWeb) try { database.accountDao().recalculateAllAccountBalances() } catch (_: Exception) { }
            restoreSettings(root)

            val totalImported = dto.categories.size + dto.accounts.size + dto.transactions.size +
                    dto.budgets.size + dto.debts.size + dto.freelanceProjects.size +
                    dto.milestones.size + dto.exchangeRates.size + dto.paymentCards.size +
                    dto.assets.size + dto.plannedPayouts.size + dto.receipts.size

            Result.success(totalImported)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
