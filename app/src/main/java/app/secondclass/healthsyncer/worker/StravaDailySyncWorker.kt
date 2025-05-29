package app.secondclass.healthsyncer.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.secondclass.healthsyncer.data.strava.StravaSyncHelper
import app.secondclass.healthsyncer.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class StravaDailySyncWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val stravaSyncHelper: StravaSyncHelper,
    ) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            val openAppIntent = NotificationHelper.createOpenAppIntent(applicationContext)
            stravaSyncHelper.restoreStravaIds()
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
