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
        TODO("Implement coalesced sync of all providers")
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
        TODO("Implement low priority background task")
    }
}
