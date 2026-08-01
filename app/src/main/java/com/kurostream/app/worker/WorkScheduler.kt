package com.kurostream.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {
    fun scheduleAll(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val cacheWork = PeriodicWorkRequestBuilder<CacheCleanupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        val syncWork = PeriodicWorkRequestBuilder<WatchProgressSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).apply {
            enqueueUniquePeriodicWork(
                CacheCleanupWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                cacheWork
            )
            enqueueUniquePeriodicWork(
                WatchProgressSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncWork
            )
        }
    }
}
