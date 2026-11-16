package com.example.util

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.sync.DataSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Demo build only: on first launch, imports the fictional sample data bundled as
 * assets/demo_backup.json (present only in the "demo" build type). No-op in real builds.
 */
object DemoSeeder {
    fun seedIfNeeded(context: Context) {
        if (BuildConfig.BUILD_TYPE != "demo") return
        val prefs = context.getSharedPreferences("demo_seed", Context.MODE_PRIVATE)
        if (prefs.getBoolean("seeded", false)) return
        val json = try {
            context.assets.open("demo_backup.json").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            return
        }
        runBlocking(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            DataSyncManager(db, context.applicationContext).importStateFromJson(json)
            db.accountDao().recalculateAllAccountBalances()
        }
        prefs.edit().putBoolean("seeded", true).apply()
    }
}
