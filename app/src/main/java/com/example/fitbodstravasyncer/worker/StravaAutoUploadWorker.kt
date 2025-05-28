package com.example.fitbodstravasyncer.worker

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.work.*
import com.example.fitbodstravasyncer.data.db.AppDatabase
import com.example.fitbodstravasyncer.data.fitbod.FitbodFetcher
import com.example.fitbodstravasyncer.ui.UiStrings
import com.example.fitbodstravasyncer.util.ApiRateLimitUtil
import com.example.fitbodstravasyncer.util.NotificationHelper
import com.example.fitbodstravasyncer.util.StravaPrefs
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StravaAutoUploadWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    companion object {
        private const val TAG = "STRAVA-auto"
        const val WORK_NAME = "auto_strava_upload"

        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<StravaAutoUploadWorker>(15, TimeUnit.MINUTES)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (StravaPrefs.isUserApiLimitNear(applicationContext)) {
            NotificationHelper.showNotification(
                applicationContext,
                "API Usage High",
                "Auto-sync paused: Nearing your Strava API limit.",
                10200
            )
            return@withContext Result.retry()
        }

        if (StravaPrefs.getApiLimitReset(applicationContext) > System.currentTimeMillis()) {
            val hint = ApiRateLimitUtil.getApiResetTimeHint(applicationContext)
            NotificationHelper.showNotification(
                applicationContext,
                "API Limit",
                "Uploads paused. Try again in $hint.",
                10001
            )
            return@withContext Result.retry()
        }

        try {
            val context = applicationContext
            val dao = AppDatabase.getInstance(context).sessionDao()
            val healthClient = HealthConnectClient.getOrCreate(context)
            val nowInstant = Instant.now()
            val startInstant = nowInstant.minusSeconds(24 * 3600)

            // DRY: Fetch and match sessions with Strava in one call
            val matchedSessions = FitbodFetcher.fetchFitbodSessionsWithStrava(
                context = context,
                healthClient = healthClient,
                startInstant = startInstant,
                endInstant = nowInstant,
                toleranceSeconds = 300,
                onRateLimit = { isAppLimit ->
                    NotificationHelper.showNotification(
                        applicationContext,
                        "Strava API Rate Limit",
                        if (isAppLimit)
                            "App-wide Strava rate limit hit. Auto-sync paused."
                        else
                            "Your Strava user rate limit hit. Auto-sync paused.",
                        10201
                    )
                },
                onUnauthorized = {
                    NotificationHelper.showNotification(
                        applicationContext,
                        "Strava Login Required",
                        "Auto-sync paused: Please reconnect Strava in the app.",
                        10202
                    )
                },
                onOtherError = { e ->
                    NotificationHelper.showNotification(
                        applicationContext,
                        "Auto-sync failed",
                        "Network or API error: ${e.message}",
                        10203
                    )
                }
            )

            // Check if there are new sessions not already in the DB
            val existingSessions = dao.getAllOnce().map { it.id }.toSet()
            val trulyNewSessions = matchedSessions.filter { it.id !in existingSessions }

            if (trulyNewSessions.isEmpty()) {
                // Nothing new: NO Strava upload calls at all!
                return@withContext Result.success()
            }

            trulyNewSessions.forEach { dao.insert(it) }

            // Only upload sessions not already matched to Strava
            val toUpload = trulyNewSessions.filter { it.stravaId == null }
            if (toUpload.isNotEmpty()) {
                toUpload.forEach { session ->
                    StravaUploadWorker.enqueue(context, session.id)
                }
                NotificationHelper.showNotification(
                    applicationContext,
                    UiStrings.AUTO_SYNC_NOTIFICATION_TITLE,
                    "${toUpload.size} new Fitbod session(s) uploaded to Strava.",
                    10125
                )
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Auto-sync failed", e)
            NotificationHelper.showNotification(
                applicationContext,
                "Auto-sync failed",
                "An unexpected error occurred: ${e.message}",
                10299
            )
            Result.retry()
        }
    }
}
