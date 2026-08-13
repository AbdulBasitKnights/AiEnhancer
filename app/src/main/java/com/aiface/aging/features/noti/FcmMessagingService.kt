package com.aiface.aging.features.noti

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives Firebase push messages and new FCM tokens.
 *
 * - Notification payload + app in background → system tray (default channel).
 * - Foreground / data-only → [onMessageReceived] builds local notification.
 */
class FcmMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM onNewToken: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM from=${message.from} data=${message.data}")

        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(com.aiface.aging.R.string.app_name)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: message.data["message"]
            ?: return

        FcmPushHelper.showNotification(
            context = this,
            title = title,
            body = body,
            data = message.data,
        )
    }

    companion object {
        private const val TAG = "FcmPush"
    }
}
