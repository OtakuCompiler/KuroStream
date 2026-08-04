package com.kurostream.app.fcm

import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kurostream.app.MainActivity
import com.kurostream.app.notification.NotificationChannels
import timber.log.Timber

class FcmMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data

        when (message.data["type"]) {
            "NEW_EPISODE" -> handleNewEpisode(data)
            "SYNC_COMPLETE" -> handleSyncComplete(data)
            "RECOMMENDATION" -> handleRecommendation(data)
            else -> showGenericNotification(message)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM token refreshed: $token")
        sendTokenToServer(token)
    }

    private fun handleNewEpisode(data: Map<String, String>) {
        val showTitle = data["show_title"] ?: return
        val episodeTitle = data["episode_title"] ?: "New Episode"
        val episodeNumber = data["episode_number"] ?: ""
        val mediaId = data["media_id"] ?: ""

        NotificationChannels.showNewEpisodeNotification(
            context = this,
            showTitle = showTitle,
            episodeTitle = episodeTitle,
            episodeNumber = episodeNumber,
        )

        // Store for deep linking
        val intent = Intent(this, MainActivity::class.java)
        intent.action = Intent.ACTION_VIEW
        intent.data = android.net.Uri.parse("kurostream://details/$mediaId")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        // The notification click will handle the deep link
    }

    private fun handleSyncComplete(data: Map<String, String>) {
        val message = data["message"] ?: "Sync completed"
        NotificationCompat.Builder(this, NotificationChannels.SYNC)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Sync Complete")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
            .also { NotificationManagerCompat.from(this).notify("sync".hashCode(), it) }
    }

    private fun handleRecommendation(data: Map<String, String>) {
        val title = data["title"] ?: "New Recommendation"
        val body = data["body"] ?: "Check out this new show!"

        NotificationCompat.Builder(this, NotificationChannels.GENERAL)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
            .also { NotificationManagerCompat.from(this).notify("recommendation".hashCode(), it) }
    }

    private fun showGenericNotification(message: RemoteMessage) {
        val notification = message.notification ?: return
        NotificationCompat.Builder(this, NotificationChannels.GENERAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notification.title ?: "KuroStream")
            .setContentText(notification.body ?: "")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
            .also { NotificationManagerCompat.from(this).notify(message.messageId.hashCode(), it) }
    }

    private fun sendTokenToServer(token: String) {
        // Send FCM token to your backend for per-user targeting
        // This would typically call your API
        Timber.d("Sending FCM token to server: $token")
    }
}