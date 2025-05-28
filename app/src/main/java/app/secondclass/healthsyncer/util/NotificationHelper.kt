package app.secondclass.healthsyncer.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import app.secondclass.healthsyncer.R

object NotificationHelper {
    private const val CHANNEL_ID = "strava_sync_channel"
    private const val CHANNEL_NAME = "Strava Sync Notifications"

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        intent: Intent? = null // default is null, so old calls don't change
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel if needed (Android 8+)
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        // Only add PendingIntent if intent is provided
        val contentIntent = intent?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
        if (contentIntent != null) builder.setContentIntent(contentIntent)

        notificationManager.notify(notificationId, builder.build())
    }

    fun createOpenAppIntent(context: Context): Intent =
        Intent(context, app.secondclass.healthsyncer.ui.main.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }


}


