package com.aiface.aging.features.noti

import android.Manifest
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.aiface.aging.R
import androidx.work.*
import com.aiface.aging.SplashActivity

class DailyNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {


    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun doWork(): Result {
        return try {

            if (!isAppInForeground()) {
                showNotification()
            }


            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun isAppInForeground(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    }


    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification() {

        val title = context.getString(R.string.daily_noti_title)
        val desc = context.getString(R.string.daily_noti_desc)
        val img = com.aiface.aging.R.drawable.splash_logo


        val channelId = "daily-noti-id"
        val channelName = "daily-noti-name"
        val notificationId = 7754

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies user after inactivity"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("isChurnNoti", true)
        }


        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val collapsedView = RemoteViews(context.packageName, R.layout.layout_notii_collpased)
        collapsedView.setTextViewText(R.id.title, title)
        collapsedView.setTextViewText(R.id.description, desc)

        context.let {
            collapsedView.setImageViewResource(R.id.rightImage, img)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.noti_icon)
            .setContentTitle(title)
            .setContentText(desc)
            .setCustomContentView(collapsedView) // collapsed
        //    .setCustomBigContentView(expandedView) // expanded
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

}
