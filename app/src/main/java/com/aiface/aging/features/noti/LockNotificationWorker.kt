package com.aiface.aging.features.noti

import android.Manifest
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.aiface.aging.R
import com.aiface.aging.SplashActivity
import java.util.Locale

class LockNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {


    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun doWork(): Result {
        return try {

           // val powerManager = context.getSystemService(PowerManager::class.java)
            val keyguardManager = context.getSystemService(KeyguardManager::class.java)
            if (keyguardManager.isKeyguardLocked) {
                if (Settings.canDrawOverlays(context)){
                    showNotification()
                }else{
                    showNormalNotification()
                }


            }else{
                showNormalNotification()
            }


            Result.success()
        } catch (e: Exception) {
            Log.e("ReminderWorker", "Error running ReminderWorker: ${e.localizedMessage}", e)
            Result.retry()
        }
    }


    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification() {

        val lockscreenIntent = Intent(context, LockScreenActivity::class.java)
        lockscreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val lockscreenPendingIntent = PendingIntent.getActivity(context, 0, lockscreenIntent, PendingIntent.FLAG_IMMUTABLE)

        var title = context.getString(R.string.make_your_photos_pop)
        var desc = context.getString(R.string.edit_enhance_and_shine_in_seconds)
        var img  = R.drawable.img_lock_noti

        val channelId = "lock-noti_id"
        val channelName = "lock-noti-name"
        val notificationId = 1002

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
            putExtra("isLockNoti", true)
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


        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.noti_icon)
            .setContentTitle(title)
            .setContentText(desc)
            .setCustomContentView(collapsedView) // collapsed
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(lockscreenPendingIntent, true)

            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }


    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNormalNotification() {

        val lockscreenIntent = Intent(context, SplashActivity::class.java)
        lockscreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val lockscreenPendingIntent = PendingIntent.getActivity(context, 0, lockscreenIntent, PendingIntent.FLAG_IMMUTABLE)



        var title = context.getString(R.string.make_your_photos_pop)
        var desc = context.getString(R.string.edit_enhance_and_shine_in_seconds)
        var img  =  R.drawable.img_lock_noti



        val channelId = "lock-noti_id"
        val channelName = "lock-noti-name"
        val notificationId = 1002

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
            putExtra("isLockNoti", true)
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


        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.noti_icon)
            .setContentTitle(title)
            .setContentText(desc)
            .setCustomContentView(collapsedView) // collapsed
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(lockscreenPendingIntent, true)

            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }


}
