package com.kurostream.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class CacheMaintenanceWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Trim Coil disk cache
            try {
                val cacheDir = applicationContext.cacheDir.resolve("image_cache")
                if (cacheDir.exists()) {
                    val totalBytes = cacheDir.walkTopDown().sumOf { it.length() }
                    val maxBytes = 50L * 1024 * 1024 // 50MB max
                    if (totalBytes > maxBytes) {
                        val oldestFiles = cacheDir.listFiles()
                            ?.sortedBy { it.lastModified() }
                            ?.dropLast(50) // keep 50 most recent
                        oldestFiles?.forEach { it.delete() }
                    }
                }
            } catch (_: Exception) { /* best-effort */ }

            try {
                val tmpDir = applicationContext.cacheDir.resolve("tmp")
                if (tmpDir.exists()) {
                    val deadline = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
                    tmpDir.listFiles()?.forEach { file ->
                        if (file.lastModified() < deadline) file.delete()
                    }
                }
            } catch (_: Exception) { /* best-effort */ }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "cache_maintenance"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<CacheMaintenanceWorker>(
                12, TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
