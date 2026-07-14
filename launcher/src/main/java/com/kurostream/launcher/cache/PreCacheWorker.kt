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
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile

@HiltWorker
class PreCacheWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val cacheEntryDao: com.kurostream.launcher.cache.CacheEntryDao
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_MEDIA_URL = "media_url"
        const val KEY_EPISODE_ID = "episode_id"
        const val KEY_SERIES_ID = "series_id"
        const val KEY_CACHE_DIR = "cache_dir"
        private const val BUFFER_SIZE = 8192
        private const val PROGRESS_UPDATE_INTERVAL = 500L
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val mediaUrl = inputData.getString(KEY_MEDIA_URL) ?: return@withContext Result.failure()
        val episodeId = inputData.getString(KEY_EPISODE_ID) ?: return@withContext Result.failure()
        val cacheDir = inputData.getString(KEY_CACHE_DIR) ?: return@withContext Result.failure()

        try {
            cacheEntryDao.updateStatus(episodeId, CacheStatus.DOWNLOADING)

            val cacheFile = File(cacheDir, "$episodeId.cache")
            val tempFile = File(cacheDir, "$episodeId.tmp")

            // Check if already partially downloaded
            val existingBytes = if (cacheFile.exists()) cacheFile.length() else 0L

            val request = Request.Builder()
                .url(mediaUrl)
                .apply {
                    if (existingBytes > 0) {
                        header("Range", "bytes=$existingBytes-")
                    }
                }
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    cacheEntryDao.updateStatus(episodeId, CacheStatus.FAILED)
                    return@withContext Result.retry()
                }

                val body = response.body ?: run {
                    cacheEntryDao.updateStatus(episodeId, CacheStatus.FAILED)
                    return@withContext Result.failure()
                }

                val totalBytes = body.contentLength() + existingBytes
                val sink = if (existingBytes > 0) {
                    RandomAccessFile(tempFile, "rw").apply { seek(existingBytes) }
                } else {
                    tempFile.outputStream().let { RandomAccessFile(tempFile, "rw") }
                }

                var downloadedBytes = existingBytes
                val buffer = ByteArray(BUFFER_SIZE)
                var lastProgressUpdate = System.currentTimeMillis()

                body.byteStream().use { input ->
                    while (isActive) {
                        val read = input.read(buffer)
                        if (read == -1) break

                        sink.write(buffer, 0, read)
                        downloadedBytes += read

                        // Update progress periodically
                        val now = System.currentTimeMillis()
                        if (now - lastProgressUpdate > PROGRESS_UPDATE_INTERVAL) {
                            val progress = if (totalBytes > 0) {
                                (downloadedBytes.toFloat() / totalBytes * 100).toInt()
                            } else 0

                            setProgress(
                                androidx.work.Data.Builder()
                                    .putInt("progress", progress)
                                    .putLong("downloaded", downloadedBytes)
                                    .putLong("total", totalBytes)
                                    .build()
                            )
                            lastProgressUpdate = now
                        }
                    }
                }

                sink.close()

                // Move temp file to final location
                if (tempFile.exists()) {
                    tempFile.renameTo(cacheFile)
                }

                cacheEntryDao.updateStatus(episodeId, CacheStatus.COMPLETED)
                cacheEntryDao.updateLastAccessed(episodeId, System.currentTimeMillis())

                Result.success()
            }
        } catch (e: Exception) {
            cacheEntryDao.updateStatus(episodeId, CacheStatus.FAILED)
            Result.retry()
        }
    }
}
