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

package com.kurostream.torrent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.kurostream.torrent.domain.*
import com.kurostream.torrent.engine.TorrentEngine
import com.kurostream.app.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

@AndroidEntryPoint
class TorrentService : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "kurostream_torrent_service"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PAUSE_ALL = "com.kurostream.torrent.action.PAUSE_ALL"
        const val ACTION_RESUME_ALL = "com.kurostream.torrent.action.RESUME_ALL"
        const val ACTION_STOP_SERVICE = "com.kurostream.torrent.action.STOP_SERVICE"
        const val ACTION_OPEN_APP = "com.kurostream.torrent.action.OPEN_APP"
        const val EXTRA_TORRENT_INFO_HASH = "extra_info_hash"
    }

    @Inject
    lateinit var engine: TorrentEngine

    private var notificationManager: NotificationManager? = null
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var activeTorrentsCount = 0
    private var isSeedingOnly = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        engine.start()
        observeEngine()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_PAUSE_ALL -> engine.pauseAll()
                ACTION_RESUME_ALL -> engine.resumeAll()
                ACTION_STOP_SERVICE -> stopSelf()
                ACTION_OPEN_APP -> openApp()
            }
        }
        updateNotification()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        engine.stop()
        stopForeground(true)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Torrent Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background torrent download and seeding"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun observeEngine() {
        scope.launch {
            engine.observeTorrents().collect { torrents ->
                activeTorrentsCount = torrents.size
                isSeedingOnly = torrents.all { it.status == TorrentStatus.SEEDING }
                updateNotification()
                checkAutoStop(torrents)
            }
        }
    }

    private fun checkAutoStop(torrents: List<TorrentInfo>) {
        val hasActive = torrents.any { it.status in setOf(TorrentStatus.DOWNLOADING, TorrentStatus.DOWNLOADING_METADATA, TorrentStatus.CHECKING) }
        val shouldRunInBackground = getSharedPreferences("torrent_settings", MODE_PRIVATE)
            .getBoolean("seed_while_idle", true)

        if (!hasActive && !shouldRunInBackground && !isSeedingOnly) {
            stopSelf()
        }
    }

    private fun updateNotification() {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): Notification {
        val torrents = engine.getAllTorrents()
        val downloading = torrents.count { it.status == TorrentStatus.DOWNLOADING }
        val seeding = torrents.count { it.status == TorrentStatus.SEEDING }
        val paused = torrents.count { it.status == TorrentStatus.PAUSED }
        val error = torrents.count { it.status == TorrentStatus.ERROR }
        val totalSpeed = torrents.sumOf { it.downloadSpeed }
        val totalUpload = torrents.sumOf { it.uploadSpeed }

        val title = when {
            downloading > 0 -> "Downloading $downloading torrent${if (downloading > 1) "s" else ""}"
            seeding > 0 -> "Seeding $seeding torrent${if (seeding > 1) "s" else ""}"
            paused > 0 -> "$paused torrent${if (paused > 1) "s" else ""} paused"
            error > 0 -> "$error torrent${if (error > 1) "s" else ""} with errors"
            else -> "Torrent service running"
        }

        val contentText = when {
            downloading > 0 -> formatSpeed(totalSpeed) + " ↓  " + formatSpeed(totalUpload) + " ↑"
            seeding > 0 -> "Seeding: " + formatSpeed(totalUpload) + " ↑"
            else -> "Idle"
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                putExtra("open_torrents", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseAction = createAction(ACTION_PAUSE_ALL, "Pause All", android.R.drawable.ic_media_pause)
        val resumeAction = createAction(ACTION_RESUME_ALL, "Resume All", android.R.drawable.ic_media_play)
        val stopAction = createAction(ACTION_STOP_SERVICE, "Stop Service", android.R.drawable.ic_menu_close_clear_cancel)
        val openAction = createAction(ACTION_OPEN_APP, "Open App", android.R.drawable.ic_menu_preferences)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setShowWhen(false)
            .addAction(pauseAction)
            .addAction(resumeAction)
            .addAction(openAction)
            .addAction(stopAction)
            .setStyle(NotificationCompat.BigTextStyle().bigText(buildDetailedText(torrents)))
            .build()
    }

    private fun createAction(action: String, title: String, icon: Int): NotificationCompat.Action {
        val intent = Intent(this, TorrentService::class.java).setAction(action)
        val pendingIntent = PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(icon, title, pendingIntent).build()
    }

    private fun buildDetailedText(torrents: List<TorrentInfo>): String {
        return torrents.take(5).joinToString("\n") { torrent ->
            val statusIcon = when (torrent.status) {
                TorrentStatus.DOWNLOADING -> "⬇"
                TorrentStatus.SEEDING -> "⬆"
                TorrentStatus.PAUSED -> "⏸"
                TorrentStatus.ERROR -> "❌"
                TorrentStatus.FINISHED -> "✓"
                TorrentStatus.DOWNLOADING_METADATA -> "🔍"
                TorrentStatus.CHECKING -> "🔄"
                else -> "?"
            }
            "$statusIcon ${torrent.name.take(40)} ${formatProgress(torrent.progress)} ${formatSpeed(torrent.downloadSpeed)}"
        }
    }

    private fun formatProgress(progress: Float): String = "(${String.format("%.1f%%", progress * 100)})"

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec < 1024 -> "${bytesPerSec} B/s"
            bytesPerSec < 1024 * 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
            bytesPerSec < 1024 * 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024))
            else -> String.format("%.1f GB/s", bytesPerSec / (1024.0 * 1024 * 1024))
        }
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra("open_torrents", true)
        startActivity(intent)
    }
}