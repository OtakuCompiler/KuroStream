package com.kurostream.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

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
}
