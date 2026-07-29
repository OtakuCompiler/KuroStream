package com.kurostream.common.optimization

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import timber.log.Timber

class CoalescedSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            performCoalescedSync()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun performCoalescedSync() {
        val inputData = inputData
        val syncType = inputData.getString("sync_type") ?: "default"
        Timber.d("Coalesced sync starting: type=$syncType")
        // Batch sync operations coalesced by WorkManager
        kotlinx.coroutines.delay(100)
        Timber.d("Coalesced sync complete: type=$syncType")
    }
}

class LowPriorityWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            performLowPriorityTask()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun performLowPriorityTask() {
        val taskType = inputData.getString("task_type") ?: "default"
        Timber.d("Low priority task starting: type=$taskType")
        kotlinx.coroutines.delay(50)
        Timber.d("Low priority task complete: type=$taskType")
    }
}
