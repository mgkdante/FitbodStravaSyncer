package app.secondclass.healthsyncer.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.secondclass.healthsyncer.data.strava.restoreStravaIds
import app.secondclass.healthsyncer.util.NotificationHelper

class StravaDailySyncWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            val openAppIntent = NotificationHelper.createOpenAppIntent(applicationContext)
            restoreStravaIds(applicationContext)
            // Notify the user when the daily sync is done
            NotificationHelper.showNotification(
                applicationContext,
                UiStrings.DAILY_SYNC_NOTIFICATION_TITLE,
                UiStrings.DAILY_SYNC_NOTIFICATION_BODY,
                10124, // Notification ID; pick any unique int
                openAppIntent
            )
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
