package com.kurostream.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kurostream.cache.KuroCacheManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class CacheCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cacheManager: KuroCacheManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            cacheManager.clearAllCaches()
            Timber.d("CacheCleanupWorker: cleared VOD cache")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "CacheCleanupWorker failed")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "cache_cleanup"
    }
}
