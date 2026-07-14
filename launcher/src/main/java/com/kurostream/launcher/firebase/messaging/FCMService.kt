package com.kurostream.launcher.firebase.messaging

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        message.notification?.let { notification ->
            NotificationHelper(this).showNotification(
                title = notification.title ?: "",
                body = notification.body ?: ""
            )
        }
    }
}
