package com.kurostream.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kurostream.cache.KuroCacheManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class CacheMaintenanceWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val kuroCacheManager: KuroCacheManager,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            kuroCacheManager.enforceBudget(500L * 1024 * 1024)
            trimCoilCache()
            cleanTempFiles()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun trimCoilCache() {
        val cacheDir = applicationContext.cacheDir.resolve("image_cache")
        if (!cacheDir.exists()) return
        val maxBytes = 25L * 1024 * 1024
        var totalBytes = 0L
        val files = cacheDir.walkTopDown()
            .filter { it.isFile }
            .sortedByDescending { it.lastModified() }
            .toList()
        for (file in files) {
            totalBytes += file.length()
            if (totalBytes > maxBytes) {
                file.delete()
            }
        }
    }

    private fun cleanTempFiles() {
        val tmpDir = applicationContext.cacheDir.resolve("tmp")
        if (!tmpDir.exists()) return
        val deadline = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        tmpDir.listFiles()?.forEach { file ->
            if (file.lastModified() < deadline) file.delete()
        }
    }
}
