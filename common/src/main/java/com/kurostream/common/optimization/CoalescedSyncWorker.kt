package com.kurostream.common.optimization

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

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
        throw UnsupportedOperationException("Coalesced sync not yet implemented")
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
        throw UnsupportedOperationException("Low priority task not yet implemented")
    }
}
