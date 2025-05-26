package com.example.fitbodstravasyncer.worker

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.*
import com.example.fitbodstravasyncer.core.network.RetrofitProvider
import com.example.fitbodstravasyncer.data.db.AppDatabase
import com.example.fitbodstravasyncer.data.strava.StravaActivityService
import com.example.fitbodstravasyncer.util.TcxFileGenerator
import androidx.work.workDataOf
import com.example.fitbodstravasyncer.util.NotificationHelper
import com.example.fitbodstravasyncer.util.StravaTokenManager
import com.example.fitbodstravasyncer.util.isStravaConnected
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class StravaUploadWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    private val ctx = applicationContext

    companion object {
        private const val TAG = "STRAVA-sync"

        /** Enqueue a one-shot upload for the given session ID */
        fun enqueue(context: Context, sessionId: String?) {
            if (sessionId.isNullOrBlank()) {
                Toast.makeText(context, "Invalid session for sync.", Toast.LENGTH_SHORT).show()
                return
            }
            if (!context.isStravaConnected()) {
                Toast.makeText(context, "Connect Strava first", Toast.LENGTH_SHORT).show()
                return
            }
            val inputData = workDataOf("SESSION_ID" to sessionId)

            WorkManager.getInstance(context).enqueueUniqueWork(
                "UPLOAD_$sessionId",
                ExistingWorkPolicy.KEEP, // Use KEEP for robustness against double-tap, prevents race!
                OneTimeWorkRequestBuilder<StravaUploadWorker>()
                    .setInputData(inputData)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30,
                        TimeUnit.SECONDS
                    )
                    .build()
            )
        }
    }

    /* ───────────────────────────── doWork() ───────────────────────────── */
    override suspend fun doWork(): Result {
        var tcxFile: java.io.File? = null // <-- For cleanup
        try {
            val sessionId = inputData.getString("SESSION_ID")?.takeIf { it.isNotBlank() } ?: run {
                Log.e(TAG, "doWork: SESSION_ID missing!")
                return Result.failure()
            }

            val dao = AppDatabase.getInstance(ctx).sessionDao()
            val session = dao.getById(sessionId) ?: run {
                Log.e(TAG, "doWork: session not found in DB for id=$sessionId")
                return Result.failure()
            }
            val notificationId = session.id.hashCode()
            // 🔔 Notify: Upload is starting
            NotificationHelper.showNotification(
                ctx,
                "Syncing to Strava",
                "Uploading workout: ${session.title}...",
                notificationId
            )

            // Local DB sanity check
            if (session.stravaId != null) {
                NotificationHelper.showNotification(
                    ctx,
                    "Strava Sync Complete",
                    "Workout already uploaded: ${session.title}",
                    notificationId
                )
                return Result.success()
            }

            // Remote Strava check
            val token = "Bearer ${StravaTokenManager.getValidAccessToken(ctx)}"
            val api = RetrofitProvider.retrofit.create(StravaActivityService::class.java)
            val formatter = java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(java.time.ZoneOffset.UTC)
            val sessionStartEpoch = session.startTime.epochSecond
            val tolerance = 300L // 5 min

            val recentActivities = api.listActivities(token, 50, 1)

            val matchingActivity = recentActivities.firstOrNull { activity ->
                activity.startDate?.let {
                    val actEpoch = java.time.Instant.from(formatter.parse(it)).epochSecond
                    kotlin.math.abs(actEpoch - sessionStartEpoch) < tolerance
                } == true
            }

            if (matchingActivity != null) {
                matchingActivity.id?.let { dao.updateStravaId(session.id, it) }
                NotificationHelper.showNotification(
                    ctx,
                    "Strava Sync Complete",
                    "Workout already uploaded on Strava: ${session.title}",
                    notificationId
                )
                return Result.success()
            }

            // ------- build TCX (summary only) -------
            tcxFile = TcxFileGenerator.generateTcxFile(
                ctx,
                session.id,
                session.title,
                session.description,
                session.startTime,
                session.activeTime * 60f,
                session.calories.toFloat(),
                session.avgHeartRate?.toFloat(),
                session.heartRateSeries
            )
            // 1️⃣ upload
            val dataType: okhttp3.RequestBody = "tcx".toRequestBody()
            val sportType: okhttp3.RequestBody = "WeightTraining".toRequestBody()
            val name: okhttp3.RequestBody = session.title.toRequestBody()
            val description: okhttp3.RequestBody = session.description.toRequestBody()

            val response: com.example.fitbodstravasyncer.data.strava.StravaUploadResponse = api.uploadActivity(
                token,
                okhttp3.MultipartBody.Part.createFormData(
                    "file",
                    "${session.id}.tcx",
                    tcxFile.asRequestBody("application/xml".toMediaType())
                ),
                dataType,
                sportType,
                name,
                description
            )
            val uploadId = response.id

            // 2️⃣ poll until Strava finishes processing with exponential backoff
            val maxChecks = 60              // 60 × 4 s  ≈ 4 min
            var checks = 0
            var delayTime = 4000L // Start with 4 seconds
            val maxDelay = 60000L // Cap delay at 60 seconds
            var status: com.example.fitbodstravasyncer.data.strava.StravaUploadStatusResponse

            do {
                kotlinx.coroutines.delay(delayTime)
                status = api.getUploadStatus(token, uploadId)
                delayTime = (delayTime * 2).coerceAtMost(maxDelay)
                checks++
            } while (status.activity_id == null && checks < maxChecks)

            if (status.activity_id != null) {
                dao.updateStravaId(sessionId, status.activity_id)

                if (tcxFile.exists()) {
                    val deleted = tcxFile.delete()
                }

                NotificationHelper.showNotification(
                    ctx,
                    "Strava Sync Complete",
                    "Workout uploaded: ${session.title}",
                    notificationId
                )
                return Result.success()
            } else {
                if (tcxFile.exists()) {
                    val deleted = tcxFile.delete()
                }
                NotificationHelper.showNotification(
                    ctx,
                    "Strava Sync Failed",
                    "Failed to upload workout: ${session.title}. Will retry.",
                    2
                )
                return Result.retry()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Clean up TCX file if cancelled
            if (tcxFile != null && tcxFile.exists()) {
                val deleted = tcxFile.delete()
            }
            NotificationHelper.showNotification(
                ctx,
                "Strava Disconnected",
                "Please reconnect your Strava account to continue syncing.",
                1
            )
            return Result.failure()
        } catch (e: Exception) {
            if (tcxFile != null && tcxFile.exists()) {
                val deleted = tcxFile.delete()
            }
            NotificationHelper.showNotification(
                ctx,
                "Strava Sync Failed",
                "Failed to upload workout. Will retry.",
                2
            )
            return Result.retry()
        }
    }

}
