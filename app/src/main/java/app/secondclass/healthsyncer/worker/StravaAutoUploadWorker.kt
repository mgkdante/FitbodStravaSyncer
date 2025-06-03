package app.secondclass.healthsyncer.worker

import UiStrings.AUTO_SYNC_NOTIFICATION_TITLE
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresExtension
import androidx.health.connect.client.HealthConnectClient
import androidx.work.*
import app.secondclass.healthsyncer.data.db.AppDatabase
import app.secondclass.healthsyncer.data.fitbod.FitbodFetcher
import app.secondclass.healthsyncer.util.ApiRateLimitUtil
import app.secondclass.healthsyncer.util.NotificationHelper
import app.secondclass.healthsyncer.util.StravaPrefs
import app.secondclass.healthsyncer.util.hasRequiredHealthPermissions
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

    @RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 13)
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!hasRequiredHealthPermissions(applicationContext)) {
            val openAppIntent = NotificationHelper.createOpenAppIntent(applicationContext)
            NotificationHelper.showNotification(
                applicationContext,
                "Permissions Needed",
                "Auto-sync failed: Please grant all Health Connect permissions (including background & from other apps).",
                10300,
                openAppIntent
            )
            return@withContext Result.failure()
        }

        val openAppIntent = NotificationHelper.createOpenAppIntent(applicationContext)

        if (StravaPrefs.isUserApiLimitNear(applicationContext)) {
            NotificationHelper.showNotification(
                applicationContext,
                "API Usage High",
                "Auto-sync paused: Nearing your Strava API limit.",
                10200,
                openAppIntent
            )
            return@withContext Result.retry()
        }

        if (StravaPrefs.getApiLimitReset(applicationContext) > System.currentTimeMillis()) {
            val hint = ApiRateLimitUtil.getApiResetTimeHint(applicationContext)
            NotificationHelper.showNotification(
                applicationContext,
                "API Limit",
                "Uploads paused. Try again in $hint.",
                10001,
                openAppIntent
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
                        10201,
                        openAppIntent
                    )
                },
                onUnauthorized = {
                    NotificationHelper.showNotification(
                        applicationContext,
                        "Strava Login Required",
                        "Auto-sync paused: Please reconnect Strava in the app.",
                        10202,
                        openAppIntent
                    )
                },
                onOtherError = { e ->
                    NotificationHelper.showNotification(
                        applicationContext,
                        "Auto-sync failed",
                        "Network or API error: ${e.message}",
                        10203,
                        openAppIntent
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
                    AUTO_SYNC_NOTIFICATION_TITLE,
                    "${toUpload.size} new Fitbod session(s) uploaded to Strava.",
                    10125,
                    openAppIntent
                )
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Auto-sync failed", e)
            val dnsError = e.message?.contains("Unable to resolve www.strava.com", ignoreCase = true) == true ||
                    e.message?.contains("No address associated with hostname", ignoreCase = true) == true

            return@withContext if (dnsError) {
                // Transient network/DNS error: retry silently, no user notification!
                Result.retry()
            } else {
                // Other errors: notify user
                NotificationHelper.showNotification(
                    applicationContext,
                    "Auto-sync failed",
                    "An unexpected error occurred: ${e.message}",
                    10299,
                    openAppIntent
                )
                Result.retry()
            }
        }
    }
}
