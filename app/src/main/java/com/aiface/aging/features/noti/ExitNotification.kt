package com.aiface.aging.features.noti

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aiface.aging.R
import com.aiface.aging.SplashActivity

object ExitNotification {


    var onPauseNotification = true

    fun showOfflineNotification(context: Context) {
        try {
            if (onPauseNotification == true) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channelId = "minimize_channel"

                val intent = Intent(context, SplashActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        channelId,
                        "Minimize Alert",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Heads-up alert when app is minimized"
                        enableLights(true)
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 300, 300, 300)
                        setSound(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
                        )
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.splash_logo)
                    .setContentTitle(" Enhance Quality of Your Snap")
                    .setContentText("Fast, HD and clear photos instantly")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setVibrate(longArrayOf(0, 300, 300, 300))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)

                // Force heads-up on Q+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setFullScreenIntent(pendingIntent, true)
                }

                // Use unique ID to ensure heads-up shows every time
                val notificationId = System.currentTimeMillis().toInt()
                notificationManager.notify(1234, builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}