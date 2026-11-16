package com.example.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Live market exchange rates (units per 1 USD) from the free ExchangeRate-API open endpoint
 * (https://www.exchangerate-api.com, updated daily, no key). Cached on the device so conversion
 * keeps working offline. Custom rates the user adds in Settings still take priority.
 */
object LiveRates {
    private const val URL = "https://open.er-api.com/v6/latest/USD"
    private const val PREFS = "live_rates"
    private const val MAX_AGE_MS = 6 * 60 * 60 * 1000L

    data class Snapshot(val perUsd: Map<String, Double> = emptyMap(), val updatedAt: Long = 0L, val fetchedAt: Long = 0L)

    private val _state = MutableStateFlow(Snapshot())
    val state: StateFlow<Snapshot> = _state.asStateFlow()

    @Volatile private var loaded = false
    private val client by lazy {
        OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()
    }

    /** Units of [code] per 1 USD, or null when no live rate is known. */
    fun perUsd(code: String): Double? = _state.value.perUsd[code]?.takeIf { it > 0 }

    /** Loads the cached rates once (cheap; safe to call from widgets and activities). */
    fun init(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            parse(p.getString("json", null))?.let { _state.value = it.copy(fetchedAt = p.getLong("fetched_at", 0L)) }
            loaded = true
        }
    }

    /** Fetches fresh rates when the cache is older than 6 hours (or always when [force]). Returns true on success. */
    suspend fun refresh(context: Context, force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        init(context)
        if (!force && System.currentTimeMillis() - _state.value.fetchedAt < MAX_AGE_MS) return@withContext true
        try {
            val body = client.newCall(Request.Builder().url(URL).build()).execute().use { r ->
                if (!r.isSuccessful) return@withContext false
                r.body?.string()
            } ?: return@withContext false
            val snap = parse(body) ?: return@withContext false
            val now = System.currentTimeMillis()
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("json", body).putLong("fetched_at", now).apply()
            _state.value = snap.copy(fetchedAt = now)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun parse(json: String?): Snapshot? = try {
        val o = JSONObject(json ?: return null)
        if (o.optString("result") != "success") null else {
            val r = o.getJSONObject("rates")
            val map = HashMap<String, Double>(r.length())
            r.keys().forEach { k -> r.optDouble(k).takeIf { !it.isNaN() && it > 0 }?.let { map[k.uppercase()] = it } }
            Snapshot(map, o.optLong("time_last_update_unix") * 1000L)
        }
    } catch (_: Exception) {
        null
    }
}
