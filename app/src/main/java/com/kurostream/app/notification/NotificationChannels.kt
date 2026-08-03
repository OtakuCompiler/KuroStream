package com.kurostream.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kurostream.app.MainActivity
import timber.log.Timber

object NotificationChannels {
    const val DOWNLOADS = "downloads"
    const val PLAYBACK = "playback"
    const val SYNC = "sync"
    const val GENERAL = "general"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val downloads = NotificationChannel(DOWNLOADS, "Downloads", NotificationManager.IMPORTANCE_LOW)
        val playback = NotificationChannel(PLAYBACK, "Playback", NotificationManager.IMPORTANCE_LOW)
        val sync = NotificationChannel(SYNC, "Sync", NotificationManager.IMPORTANCE_MIN)
        val general = NotificationChannel(GENERAL, "General", NotificationManager.IMPORTANCE_DEFAULT)

        manager.createNotificationChannels(listOf(downloads, playback, sync, general))
    }

    fun showNewEpisodeNotification(
        context: Context,
        showTitle: String,
        episodeTitle: String,
        episodeNumber: String,
    ) {
        val notificationId = ("episode_$showTitle").hashCode()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GENERAL)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("$showTitle - $episodeTitle")
            .setContentText("Episode $episodeNumber")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
        Timber.d("New episode notification: $showTitle - $episodeTitle")
    }
}
