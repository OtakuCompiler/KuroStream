package com.kurostream.common.optimization

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkManagerOptimizer {
    private const val COALESCED_SYNC_TAG = "coalesced_sync"
    private const val COALESCED_SYNC_NAME = "KuroStreamCoalescedSync"

    fun scheduleCoalescedSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(false)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<CoalescedSyncWorker>(
            4, TimeUnit.HOURS,
            15, TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.SECONDS,
            )
            .addTag(COALESCED_SYNC_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            COALESCED_SYNC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest,
        )
    }

    // LowPriorityWorker not implemented — feature disabled for v1
}
