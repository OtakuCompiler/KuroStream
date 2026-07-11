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

package com.kurostream.data.anistream.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.kurostream.legacyui.anistream.MainActivity
import com.kurostream.legacyui.anistream.R
import com.kurostream.data.anistream.downloads.DownloadManager
import com.kurostream.data.anistream.downloads.DownloadProgress
import com.kurostream.data.anistream.downloads.DownloadStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service for background downloads on Android TV.
 * Required for long-running download operations when app is backgrounded.
 */
@AndroidEntryPoint
class DownloadService : LifecycleService() {

    @Inject
    lateinit var downloadManager: DownloadManager

    private lateinit var notificationManager: NotificationManager
    private val activeNotifications = mutableMapOf<String, NotificationCompat.Builder>()

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val CHANNEL_NAME = "Downloads"
        const val NOTIFICATION_BASE_ID = 1000

        const val ACTION_START = "com.kurostream.legacyui.anistream.action.START_DOWNLOADS"
        const val ACTION_PAUSE_ALL = "com.kurostream.legacyui.anistream.action.PAUSE_ALL"
        const val ACTION_RESUME_ALL = "com.kurostream.legacyui.anistream.action.RESUME_ALL"
        const val ACTION_CANCEL_ALL = "com.kurostream.legacyui.anistream.action.CANCEL_ALL"

        fun startService(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseAll(context: Context) {
            context.startService(Intent(context, DownloadService::class.java).apply {
                action = ACTION_PAUSE_ALL
            })
        }

        fun resumeAll(context: Context) {
            context.startService(Intent(context, DownloadService::class.java).apply {
                action = ACTION_RESUME_ALL
            })
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> startForegroundService()
            ACTION_PAUSE_ALL -> lifecycleScope.launch { downloadManager.pauseAll() }
            ACTION_RESUME_ALL -> lifecycleScope.launch { downloadManager.resumeAll() }
            ACTION_CANCEL_ALL -> lifecycleScope.launch { downloadManager.pauseAll() }
        }

        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createBaseNotification()
        startForeground(NOTIFICATION_BASE_ID, notification)

        // Observe active downloads and update notifications
        lifecycleScope.launch {
            downloadManager.activeDownloads.collectLatest { downloads ->
                updateNotifications(downloads)
            }
        }
    }

    private fun updateNotifications(downloads: Map<String, DownloadProgress>) {
        if (downloads.isEmpty()) {
            // Show idle notification
            val idleNotification = createBaseNotification()
            notificationManager.notify(NOTIFICATION_BASE_ID, idleNotification)
            return
        }

        downloads.forEach { (downloadId, progress) ->
            val builder = activeNotifications.getOrPut(downloadId) {
                createDownloadNotificationBuilder(downloadId)
            }

            builder.setProgress(100, progress.percent, false)
                .setContentText("${progress.percent}% - ${formatBytes(progress.bytesDownloaded)} / ${formatBytes(progress.totalBytes)}")

            val notificationId = NOTIFICATION_BASE_ID + downloadId.hashCode()
            notificationManager.notify(notificationId, builder.build())
        }

        // Remove notifications for completed/cancelled downloads
        val activeIds = downloads.keys.map { NOTIFICATION_BASE_ID + it.hashCode() }
        activeNotifications.keys.filter { it !in downloads.keys }.forEach { id ->
            notificationManager.cancel(NOTIFICATION_BASE_ID + id.hashCode())
            activeNotifications.remove(id)
        }
    }

    private fun createBaseNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AniStream Downloads")
            .setContentText("Managing downloads...")
            .setSmallIcon(R.drawable.ic_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(R.drawable.ic_pause, "Pause All", createActionIntent(ACTION_PAUSE_ALL))
            .build()
    }

    private fun createDownloadNotificationBuilder(downloadId: String): NotificationCompat.Builder {
        val pendingIntent = PendingIntent.getActivity(
            this,
            downloadId.hashCode(),
            Intent(this, MainActivity::class.java).apply {
                putExtra("navigate_to", "downloads")
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading...")
            .setSmallIcon(R.drawable.ic_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
    }

    private fun createActionIntent(action: String): PendingIntent {
        val intent = Intent(this, DownloadService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows download progress"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> "%.2f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> "%.2f MB".format(bytes / 1_048_576.0)
            bytes >= 1024 -> "%.2f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
