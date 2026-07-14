// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.launcher.cache

import android.content.Context
import androidx.work.*
import com.kurostream.launcher.cache.CacheEntryDao
import com.kurostream.launcher.data.local.entity.CacheEntryEntity
import com.kurostream.launcher.ml.prediction.PredictionRepository
import com.kurostream.launcher.ml.prediction.PredictionRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cacheEntryDao: CacheEntryDao,
    private val predictionRepository: PredictionRepository,
    private val evictionPolicy: CacheEvictionPolicy
) {
    companion object {
        private const val MAX_CACHE_SIZE_MB = 2048L // 2GB default
        private const val PRE_CACHE_WORK_TAG = "precache_work"
        private const val EVICTION_WORK_TAG = "eviction_work"
    }

    private val workManager = WorkManager.getInstance(context)
    private val cacheDir = File(context.cacheDir, "media_cache")

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    /**
     * Schedule pre-caching based on predictions
     */
    suspend fun schedulePreCache(seriesId: String, nextEpisodeId: String, mediaUrl: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val inputData = workDataOf(
            PreCacheWorker.KEY_MEDIA_URL to mediaUrl,
            PreCacheWorker.KEY_EPISODE_ID to nextEpisodeId,
            PreCacheWorker.KEY_SERIES_ID to seriesId,
            PreCacheWorker.KEY_CACHE_DIR to cacheDir.absolutePath
        )

        val preCacheWork = OneTimeWorkRequestBuilder<PreCacheWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(PRE_CACHE_WORK_TAG)
            .addTag("series_$seriesId")
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            "precache_$nextEpisodeId",
            ExistingWorkPolicy.KEEP,
            preCacheWork
        )

        // Record scheduled cache entry
        cacheEntryDao.insert(
            CacheEntryEntity(
                id = nextEpisodeId,
                seriesId = seriesId,
                mediaUrl = mediaUrl,
                localPath = File(cacheDir, "$nextEpisodeId.cache").absolutePath,
                status = CacheStatus.SCHEDULED,
                priority = CachePriority.HIGH,
                scheduledAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Schedule batch pre-caching for multiple predicted episodes
     */
    suspend fun scheduleBatchPreCache(requests: List<PreCacheRequest>) {
        val predictions = predictionRepository.getBatchPredictions(
            requests.map { PredictionRequest(it.seriesId, it.currentEpisode, it.completionPercentage) }
        )

        predictions.filter { it.second.willWatch && it.second.confidence > 0.7f }
            .forEach { (request, prediction) ->
                schedulePreCache(
                    seriesId = request.seriesId,
                    nextEpisodeId = request.nextEpisodeId,
                    mediaUrl = request.mediaUrl
                )
            }
    }

    /**
     * Run cache eviction to free up space
     */
    suspend fun runEviction() = withContext(Dispatchers.IO) {
        val currentSize = getCacheSize()
        val maxSize = getMaxCacheSize()

        if (currentSize <= maxSize) return@withContext

        val entriesToEvict = evictionPolicy.selectForEviction(
            entries = cacheEntryDao.getAllEntries(),
            targetSize = currentSize - (maxSize * 0.8).toLong() // Free up to 80% of max
        )

        entriesToEvict.forEach { entry ->
            evictEntry(entry)
        }
    }

    /**
     * Schedule periodic eviction
     */
    fun schedulePeriodicEviction() {
        val constraints = Constraints.Builder()
            .setRequiresStorageNotLow(false)
            .build()

        val evictionWork = PeriodicWorkRequestBuilder<CacheEvictionWorker>(
            repeatInterval = 6,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(EVICTION_WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "periodic_eviction",
            ExistingPeriodicWorkPolicy.KEEP,
            evictionWork
        )
    }

    /**
     * Get cached file path if available
     */
    suspend fun getCachedFile(episodeId: String): File? = withContext(Dispatchers.IO) {
        val entry = cacheEntryDao.getEntry(episodeId) ?: return@withContext null
        if (entry.status != CacheStatus.COMPLETED) return@withContext null

        val file = File(entry.localPath)
        if (file.exists() && file.length() > 0) {
            // Update last accessed
            cacheEntryDao.updateLastAccessed(episodeId, System.currentTimeMillis())
            file
        } else {
            // File missing, mark as failed
            cacheEntryDao.updateStatus(episodeId, CacheStatus.FAILED)
            null
        }
    }

    /**
     * Check if episode is cached
     */
    suspend fun isCached(episodeId: String): Boolean {
        return cacheEntryDao.getEntry(episodeId)?.status == CacheStatus.COMPLETED
    }

    /**
     * Get cache statistics
     */
    suspend fun getCacheStats(): CacheStats = withContext(Dispatchers.IO) {
        val entries = cacheEntryDao.getAllEntries()
        val totalSize = entries.filter { it.status == CacheStatus.COMPLETED }
            .sumOf { File(it.localPath).length() }

        CacheStats(
            totalSizeBytes = totalSize,
            maxSizeBytes = getMaxCacheSize(),
            cachedItems = entries.count { it.status == CacheStatus.COMPLETED },
            scheduledItems = entries.count { it.status == CacheStatus.SCHEDULED },
            failedItems = entries.count { it.status == CacheStatus.FAILED }
        )
    }

    private fun evictEntry(entry: CacheEntryEntity) {
        val file = File(entry.localPath)
        if (file.exists()) {
            file.delete()
        }
        cacheEntryDao.delete(entry)
    }

    private fun getCacheSize(): Long {
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    private fun getMaxCacheSize(): Long {
        return MAX_CACHE_SIZE_MB * 1024 * 1024
    }

    /**
     * Clear entire cache
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cacheDir.listFiles()?.forEach { it.delete() }
        cacheEntryDao.deleteAll()
    }
}

data class PreCacheRequest(
    val seriesId: String,
    val currentEpisode: Int,
    val nextEpisodeId: String,
    val mediaUrl: String,
    val completionPercentage: Float
)

data class CacheStats(
    val totalSizeBytes: Long,
    val maxSizeBytes: Long,
    val cachedItems: Int,
    val scheduledItems: Int,
    val failedItems: Int
) {
    val usagePercentage: Float
        get() = if (maxSizeBytes > 0) (totalSizeBytes.toFloat() / maxSizeBytes) * 100 else 0f
}
