package com.kurostream.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kurostream.domain.repository.WatchProgressRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class WatchProgressSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val watchProgressRepository: WatchProgressRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            watchProgressRepository.syncPending()
            Timber.d("WatchProgressSyncWorker: synced")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "WatchProgressSyncWorker failed")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "watch_progress_sync"
    }
}
