package com.example.data.sync

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Background auto sync: Android runs this about every 15 minutes (the platform minimum) when the
 * phone has a network connection, so changes made on the web reach the phone and phone edits
 * reach the server even while the app is closed. Uses the platform JobScheduler (no extra library).
 */
class ServerSyncJobService : JobService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var running: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val manager = ServerSyncManager.get(applicationContext)
        if (!manager.isSyncEnabled) return false
        running = scope.launch {
            try {
                manager.syncNow()
            } catch (_: Exception) {
                // Recorded in the sync state; the next periodic run retries.
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        running?.cancel()
        return true
    }

    companion object {
        private const val JOB_ID = 0x5F5C
        private const val INTERVAL_MS = 15 * 60 * 1000L

        fun schedule(context: Context) {
            val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
            if (scheduler.getPendingJob(JOB_ID) != null) return
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, ServerSyncJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(INTERVAL_MS)
                .setPersisted(true) // survives reboots (needs RECEIVE_BOOT_COMPLETED)
                .build()
            try { scheduler.schedule(job) } catch (_: Exception) { }
        }

        fun cancel(context: Context) {
            context.getSystemService(JobScheduler::class.java)?.cancel(JOB_ID)
        }
    }
}
