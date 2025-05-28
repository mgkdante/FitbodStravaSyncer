package com.example.fitbodstravasyncer.worker

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.fitbodstravasyncer.data.db.AppDatabase
import com.example.fitbodstravasyncer.data.fitbod.FitbodFetcher
import com.example.fitbodstravasyncer.data.strava.StravaApiClient
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
                WORK_NAME, // use constant here
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

            // Only call Health Connect first
            val newSessions = FitbodFetcher.fetchFitbodSessions(
                healthClient,
                startInstant,
                nowInstant,
                emptyList() // pass empty Strava list, just want raw Fitbod
            )

            // Check if there are new sessions not already in the DB
            val existingSessions = dao.getAllOnce().map { it.id }.toSet()
            val trulyNewSessions = newSessions.filter { it.id !in existingSessions }

            if (trulyNewSessions.isEmpty()) {
                // Nothing new: NO STRAVA API CALLS at all!
                return@withContext Result.success()
            }

            // Only proceed with Strava API if new sessions found
            // Now fetch Strava activities
            val client = StravaApiClient(context)
            val stravaActivities = client.listAllActivities()

            // Update any sessions that match existing Strava activities (avoid duplicates)
            val sessionsToInsert = trulyNewSessions.map { session ->
                val match = stravaActivities.firstOrNull { activity ->
                    activity.startDate?.let {
                        val actEpoch = try { Instant.parse(it).epochSecond } catch (e: Exception) { null }
                        actEpoch != null && kotlin.math.abs(actEpoch - session.startTime.epochSecond) < 300
                    } == true
                }
                if (match?.id != null) session.copy(stravaId = match.id) else session
            }

            sessionsToInsert.forEach { dao.insert(it) }

            // Only upload sessions not already matched to Strava
            val toUpload = sessionsToInsert.filter { it.stravaId == null }
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
            Log.e("STRAVA-auto", "Auto-sync failed", e)
            Result.retry()
        }
    }


}
