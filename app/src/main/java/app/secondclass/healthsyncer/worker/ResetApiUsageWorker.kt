package app.secondclass.healthsyncer.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.secondclass.healthsyncer.util.StravaPrefs
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class ResetApiUsageWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = StravaPrefs.securePrefs(applicationContext)
        prefs.edit().apply {
            putInt(StravaPrefs.KEY_USER_REQUESTS_15M, 0)
            putInt(StravaPrefs.KEY_USER_REQUESTS_DAY, 0)
            putInt(StravaPrefs.KEY_USER_READS_15M, 0)
            putInt(StravaPrefs.KEY_USER_READS_DAY, 0)
            apply()
        }
        return Result.success()
    }
}


fun scheduleDailyReset(context: Context) {
    val now = ZonedDateTime.now(ZoneOffset.UTC)
    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(ZoneOffset.UTC)
    val initialDelay = Duration.between(now, nextMidnight).toMinutes()

    val dailyWorkRequest = PeriodicWorkRequestBuilder<ResetApiUsageWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(initialDelay, TimeUnit.MINUTES)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "ResetApiUsage",
        ExistingPeriodicWorkPolicy.REPLACE,
        dailyWorkRequest
    )
}