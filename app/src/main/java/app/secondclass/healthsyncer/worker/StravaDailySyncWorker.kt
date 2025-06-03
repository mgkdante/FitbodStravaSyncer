package app.secondclass.healthsyncer.worker

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.secondclass.healthsyncer.data.strava.restoreStravaIds
import app.secondclass.healthsyncer.util.NotificationHelper
import app.secondclass.healthsyncer.util.hasRequiredHealthPermissions

class StravaDailySyncWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {
    @RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 13)
    override suspend fun doWork(): Result {
        try {
            if (!hasRequiredHealthPermissions(applicationContext)) {
                val openAppIntent = NotificationHelper.createOpenAppIntent(applicationContext)
                NotificationHelper.showNotification(
                    applicationContext,
                    "Permissions Needed",
                    "Daily sync failed: Please grant all Health Connect permissions.",
                    10301,
                    openAppIntent
                )
                return Result.failure()
            }

            val openAppIntent = NotificationHelper.createOpenAppIntent(applicationContext)
            restoreStravaIds(applicationContext)
            // Notify the user when the daily sync is done
            NotificationHelper.showNotification(
                applicationContext,
                UiStrings.DAILY_SYNC_NOTIFICATION_TITLE,
                UiStrings.DAILY_SYNC_NOTIFICATION_BODY,
                10124,
                openAppIntent
            )
            return Result.success()
        } catch (e: Exception) {
            val dnsError = e.message?.contains("Unable to resolve www.strava.com", ignoreCase = true) == true ||
                    e.message?.contains("No address associated with hostname", ignoreCase = true) == true
            return if (dnsError) {
                // Retry silently, no user notification
                Result.retry()
            } else {
                // For other errors, show a notification
                val openAppIntent = NotificationHelper.createOpenAppIntent(applicationContext)
                NotificationHelper.showNotification(
                    applicationContext,
                    "Daily sync failed",
                    "An unexpected error occurred: ${e.message}",
                    10302,
                    openAppIntent
                )
                Result.retry()
            }
        }
    }
}
