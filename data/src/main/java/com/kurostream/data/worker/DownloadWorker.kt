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

package com.kurostream.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Downloads a single file to a local destination.
 *
 * Input data keys:
 * - [KEY_URL]: source URL (required)
 * - [KEY_DEST]: absolute destination path (required)
 * - [KEY_TITLE]: optional human-readable title for the foreground notification
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val okHttpClient: OkHttpClient,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL)
        val dest = inputData.getString(KEY_DEST)
        val title = inputData.getString(KEY_TITLE) ?: "Downloading"

        if (url.isNullOrBlank() || dest.isNullOrBlank()) {
            Timber.w("DownloadWorker missing required input (url=$url, dest=$dest)")
            return Result.failure()
        }

        return try {
            setForeground(buildForegroundInfo(title, 0))
            downloadFile(url, dest, title)
            Result.success()
        } catch (e: IOException) {
            Timber.e(e, "Download failed (retryable): $url")
            Result.retry()
        } catch (e: Exception) {
            Timber.e(e, "Download failed (fatal): $url")
            Result.failure()
        }
    }

    private suspend fun downloadFile(url: String, dest: String, title: String) =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    throw IOException("HTTP ${resp.code} for $url")
                }
                val body = resp.body ?: throw IOException("Empty response body for $url")

                val destFile = File(dest).apply { parentFile?.mkdirs() }
                val totalBytes = body.contentLength()

                body.byteStream().use { input ->
                    destFile.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        var downloaded = 0L
                        var lastProgress = -1

                        while (true) {
                            bytesRead = input.read(buffer)
                            if (bytesRead == -1) break
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead

                            if (totalBytes > 0) {
                                val progress = ((downloaded * 100) / totalBytes).toInt()
                                if (progress != lastProgress && progress % 5 == 0) {
                                    lastProgress = progress
                                    setProgressAsync(
                                        androidx.work.workDataOf(
                                            KEY_PROGRESS to progress,
                                            KEY_DOWNLOADED to downloaded,
                                            KEY_TOTAL to totalBytes,
                                        )
                                    )
                                    setForeground(buildForegroundInfo(title, progress))
                                }
                            }
                        }
                    }
                }
            }
        }

    private fun buildForegroundInfo(title: String, progress: Int): ForegroundInfo {
        ensureNotificationChannel()

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(if (progress > 0) "$progress%" else "In progress")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Downloads",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "File download progress"
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        const val KEY_URL = "url"
        const val KEY_DEST = "dest"
        const val KEY_TITLE = "title"
        const val KEY_PROGRESS = "progress"
        const val KEY_DOWNLOADED = "downloaded"
        const val KEY_TOTAL = "total"

        const val UNIQUE_WORK_NAME = "resume_downloads"
        const val CHANNEL_ID = "downloads"
        const val NOTIFICATION_ID = 2001

        private const val BUFFER_SIZE = 8 * 1024
    }
}
