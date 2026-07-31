package com.kurostream.app.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val CACHE_WORK_NAME = "cache_maintenance"
    private const val SYNC_WORK_NAME = "sync_worker"

    fun scheduleAll(context: Context) {
        val cacheWork = PeriodicWorkRequestBuilder<CacheMaintenanceWorker>(6, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CACHE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cacheWork
        )

        val syncWork = PeriodicWorkRequestBuilder<CoalescedSyncWorker>(12, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWork
        )
    }
}
