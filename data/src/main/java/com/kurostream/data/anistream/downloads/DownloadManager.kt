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

package com.kurostream.data.anistream.downloads

import android.content.Context
import androidx.work.*
import com.kurostream.legacyui.anistream.worker.DownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central download manager with priority queue, pause/resume/retry.
 * Uses WorkManager for persistent background download tasks.
 */
@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val workManager: WorkManager
) {

    private val _downloadQueue = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloadQueue: Flow<List<DownloadItem>> = _downloadQueue.asStateFlow()

    private val _activeDownloads = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val activeDownloads: Flow<Map<String, DownloadProgress>> = _activeDownloads.asStateFlow()

    private val maxConcurrentDownloads = 2
    private val priorityQueue = PriorityQueue<DownloadItem>(compareByDescending { it.priority })
    private val mutex = Mutex()

    init {
        restorePendingDownloads()
    }

    /**
     * Enqueue a new download with priority.
     * Priority: 0 = lowest, 10 = highest (user-initiated)
     */
    suspend fun enqueueDownload(
        animeId: String,
        episodeId: String,
        title: String,
        url: String,
        posterUrl: String? = null,
        priority: Int = 5
    ): String {
        val downloadId = UUID.randomUUID().toString()
        val item = DownloadItem(
            id = downloadId,
            animeId = animeId,
            episodeId = episodeId,
            title = title,
            url = url,
            posterUrl = posterUrl,
            priority = priority,
            status = DownloadStatus.QUEUED,
            createdAt = System.currentTimeMillis()
        )

        downloadDao.insert(item)
        priorityQueue.add(item)
        processQueue()

        return downloadId
    }

    suspend fun pauseDownload(downloadId: String) {
        val item = downloadDao.getById(downloadId) ?: return
        if (item.status != DownloadStatus.DOWNLOADING) return

        // Cancel the worker
        workManager.cancelUniqueWork("download_$downloadId")

        downloadDao.updateStatus(downloadId, DownloadStatus.PAUSED)
        _activeDownloads.update { it - downloadId }
        processQueue()
    }

    suspend fun resumeDownload(downloadId: String) {
        val item = downloadDao.getById(downloadId) ?: return
        if (item.status != DownloadStatus.PAUSED && item.status != DownloadStatus.FAILED) return

        downloadDao.updateStatus(downloadId, DownloadStatus.QUEUED)
        priorityQueue.add(item.copy(status = DownloadStatus.QUEUED))
        processQueue()
    }

    suspend fun retryDownload(downloadId: String) {
        val item = downloadDao.getById(downloadId) ?: return
        if (item.status != DownloadStatus.FAILED && item.status != DownloadStatus.CANCELLED) return

        downloadDao.updateStatus(downloadId, DownloadStatus.QUEUED)
        downloadDao.updateRetryCount(downloadId, item.retryCount + 1)
        priorityQueue.add(item.copy(
            status = DownloadStatus.QUEUED,
            retryCount = item.retryCount + 1
        ))
        processQueue()
    }

    suspend fun cancelDownload(downloadId: String) {
        workManager.cancelUniqueWork("download_$downloadId")
        downloadDao.updateStatus(downloadId, DownloadStatus.CANCELLED)
        _activeDownloads.update { it - downloadId }
        processQueue()
    }

    suspend fun removeDownload(downloadId: String) {
        cancelDownload(downloadId)
        downloadDao.delete(downloadId)
        // Delete file if exists
        val file = DownloadFileHelper.getDownloadFile(context, downloadId)
        file.delete()
    }

    suspend fun cancelAll() {
        val all = downloadDao.getAll()
        all.forEach { cancelDownload(it.id) }
    }

    suspend fun pauseAll() {
        val active = downloadDao.getByStatus(DownloadStatus.DOWNLOADING)
        active.forEach { pauseDownload(it.id) }
    }

    suspend fun resumeAll() {
        val paused = downloadDao.getByStatus(DownloadStatus.PAUSED)
        paused.forEach { resumeDownload(it.id) }
    }

    fun updateProgress(downloadId: String, progress: DownloadProgress) {
        _activeDownloads.update { it + (downloadId to progress) }
    }

    fun markComplete(downloadId: String, filePath: String) {
        _activeDownloads.update { it - downloadId }
    }

    fun markFailed(downloadId: String, error: String) {
        _activeDownloads.update { it - downloadId }
    }

    private suspend fun processQueue() {
        mutex.withLock {
            val activeCount = downloadDao.getByStatus(DownloadStatus.DOWNLOADING).size
            val availableSlots = maxConcurrentDownloads - activeCount

            if (availableSlots <= 0) return@withLock

            val queued = downloadDao.getByStatus(DownloadStatus.QUEUED)
                .sortedByDescending { it.priority }

            queued.take(availableSlots).forEach { item ->
                startDownloadWorker(item)
            }
        }
    }

    private suspend fun startDownloadWorker(item: DownloadItem) {
        downloadDao.updateStatus(item.id, DownloadStatus.DOWNLOADING)

        val inputData = workDataOf(
            DownloadWorker.KEY_DOWNLOAD_ID to item.id,
            DownloadWorker.KEY_URL to item.url,
            DownloadWorker.KEY_TITLE to item.title,
            DownloadWorker.KEY_EPISODE_ID to item.episodeId
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val downloadWork = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("download")
            .build()

        workManager.enqueueUniqueWork(
            "download_${item.id}",
            ExistingWorkPolicy.KEEP,
            downloadWork
        )
    }

    private fun restorePendingDownloads() {
        // On app startup, resume any queued/paused downloads
    }

    suspend fun getDownloadHistory(): List<DownloadItem> {
        return downloadDao.getAll()
    }

    suspend fun getDownloadById(downloadId: String): DownloadItem? {
        return downloadDao.getById(downloadId)
    }
}

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speedBps: Long = 0
) {
    val percent: Int = if (totalBytes > 0) {
        ((bytesDownloaded * 100) / totalBytes).toInt()
    } else 0
}
