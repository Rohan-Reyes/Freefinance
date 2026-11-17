package com.example.util

import android.content.Context
import com.example.BuildConfig
import com.example.data.sync.ServerSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Finds newer versions of the app on GitHub Releases (the public download) and on the sync server
 * (Settings → Server sync address). The newer of the two wins; on a tie GitHub is used.
 */
object UpdateChecker {
    const val REPO = "Rohan-Reyes/Freefinance"
    const val RELEASES_PAGE = "https://github.com/$REPO/releases"
    private const val API = "https://api.github.com/repos/$REPO/releases"
    private const val PREFS = "ff_update_check"
    private const val DAY_MS = 24 * 60 * 60 * 1000L

    data class Release(
        val versionName: String,
        val title: String,
        val notes: String,
        val publishedAt: Long,
        val apkUrl: String?,
        val sizeBytes: Long,
        val pageUrl: String,
    )

    data class Update(val versionName: String, val sizeMb: Double, val downloadUrl: String, val source: String, val notes: String)

    /** "v1.3.6" / "1.3.6-demo" -> [1, 3, 6]. */
    private fun parts(v: String): List<Int> =
        v.trim().removePrefix("v").removePrefix("V").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }

    /** > 0 when [a] is newer than [b]. */
    fun compare(a: String, b: String): Int {
        val x = parts(a); val y = parts(b)
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = (x.getOrElse(i) { 0 }) - (y.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return 0
    }

    private fun get(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "MyFinance-Android")
        }
        try {
            if (conn.responseCode !in 200..299) throw IllegalStateException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

    private fun parseRelease(o: JSONObject): Release? {
        if (o.optBoolean("draft") || o.optBoolean("prerelease")) return null
        val assets = o.optJSONArray("assets") ?: JSONArray()
        var apk: JSONObject? = null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.optString("name").endsWith(".apk", ignoreCase = true)) { apk = a; break }
        }
        val tag = o.optString("tag_name")
        return Release(
            versionName = tag.removePrefix("v"),
            title = o.optString("name").ifBlank { tag },
            notes = o.optString("body"),
            publishedAt = runCatching { isoDate.parse(o.optString("published_at"))?.time ?: 0L }.getOrDefault(0L),
            apkUrl = apk?.optString("browser_download_url")?.takeIf { it.startsWith("https://") },
            sizeBytes = apk?.optLong("size") ?: 0L,
            pageUrl = o.optString("html_url").takeIf { it.startsWith("https://") } ?: RELEASES_PAGE,
        )
    }

    /** Published releases on GitHub, newest first. */
    suspend fun githubReleases(): List<Release> = withContext(Dispatchers.IO) {
        val arr = JSONArray(get("$API?per_page=20"))
        (0 until arr.length()).mapNotNull { parseRelease(arr.getJSONObject(it)) }
            .sortedWith { a, b -> compare(b.versionName, a.versionName) }
    }

    private suspend fun githubLatest(): Update? = withContext(Dispatchers.IO) {
        val r = parseRelease(JSONObject(get("$API/latest"))) ?: return@withContext null
        val url = r.apkUrl ?: return@withContext null
        Update(r.versionName, r.sizeBytes / 1_048_576.0, url, "GitHub", r.notes)
    }

    private suspend fun serverLatest(context: Context): Update? = withContext(Dispatchers.IO) {
        val base = ServerSyncManager.get(context.applicationContext).state.value.serverUrl.trimEnd('/')
        if (!base.startsWith("https://")) return@withContext null
        val app = JSONObject(get("$base/api/app-version")).optJSONObject("app") ?: return@withContext null
        val name = app.optString("versionName").takeIf { it.isNotBlank() } ?: return@withContext null
        val path = app.optString("url", "/download/my-finance.apk")
        if (!path.startsWith("/")) return@withContext null
        Update(name, app.optLong("size", 0) / 1_048_576.0, base + path, base.removePrefix("https://"), "")
    }

    sealed class Result {
        data class Available(val update: Update) : Result()
        object UpToDate : Result()
        object Unreachable : Result()
    }

    /** Checks GitHub and the sync server in parallel. */
    suspend fun check(context: Context): Result = coroutineScope {
        val gh = async { runCatching { githubLatest() } }
        val sv = async { runCatching { serverLatest(context) } }
        val ghR = gh.await(); val svR = sv.await()
        if (ghR.isFailure && svR.isFailure) return@coroutineScope Result.Unreachable
        val best = listOfNotNull(ghR.getOrNull(), svR.getOrNull())
            .sortedWith { a, b -> compare(b.versionName, a.versionName) }
            .firstOrNull()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong("last_check", System.currentTimeMillis()).apply()
        if (best != null && compare(best.versionName, BuildConfig.VERSION_NAME) > 0) Result.Available(best) else Result.UpToDate
    }

    /** Automatic check at most once a day; returns an update the user hasn't dismissed yet. */
    suspend fun dailyCheck(context: Context): Update? {
        if (BuildConfig.BUILD_TYPE == "demo") return null
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (System.currentTimeMillis() - sp.getLong("last_check", 0L) < DAY_MS) return null
        val r = check(context) as? Result.Available ?: return null
        if (sp.getString("dismissed", null) == r.update.versionName) return null
        return r.update
    }

    fun dismiss(context: Context, versionName: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("dismissed", versionName).apply()
    }
}
