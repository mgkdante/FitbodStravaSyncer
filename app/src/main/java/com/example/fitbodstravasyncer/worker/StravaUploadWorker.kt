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
                Log.e(TAG, "enqueue: Ignoring blank/null sessionId")
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
            Log.i(TAG, "Enqueued upload for sessionId=$sessionId")
        }
    }

    /* ───────────────────────────── doWork() ───────────────────────────── */
    override suspend fun doWork(): Result {
        var tcxFile: java.io.File? = null // <-- For cleanup
        try {
            Log.i(TAG, "doWork: started")

            val sessionId = inputData.getString("SESSION_ID")?.takeIf { it.isNotBlank() } ?: run {
                Log.e(TAG, "doWork: SESSION_ID missing!")
                return Result.failure()
            }
            Log.i(TAG, "doWork: sessionId=$sessionId")

            val dao = AppDatabase.getInstance(ctx).sessionDao()
            val session = dao.getById(sessionId) ?: run {
                Log.e(TAG, "doWork: session not found in DB for id=$sessionId")
                return Result.failure()
            }
            Log.i(TAG, "doWork: loaded session from DB")

            // 🔔 Notify: Upload is starting
            NotificationHelper.showNotification(
                ctx,
                "Syncing to Strava",
                "Uploading workout: ${session.title}...",
                2
            )

            // Local DB sanity check
            if (session.stravaId != null) {
                Log.i(TAG, "doWork: already uploaded, stravaId=${session.stravaId}")
                NotificationHelper.showNotification(
                    ctx,
                    "Strava Sync Complete",
                    "Workout already uploaded: ${session.title}",
                    2
                )
                return Result.success()
            }

            // Remote Strava check
            val token = "Bearer ${StravaTokenManager.getValidAccessToken(ctx)}"
            val api = RetrofitProvider.retrofit.create(StravaActivityService::class.java)
            val formatter = java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(java.time.ZoneOffset.UTC)
            val sessionStartEpoch = session.startTime.epochSecond
            val tolerance = 300L // 5 min

            Log.i(TAG, "Fetching recent Strava activities for matching...")
            val recentActivities = api.listActivities(token, 50, 1)

            val matchingActivity = recentActivities.firstOrNull { activity ->
                activity.startDate?.let {
                    val actEpoch = java.time.Instant.from(formatter.parse(it)).epochSecond
                    kotlin.math.abs(actEpoch - sessionStartEpoch) < tolerance
                } ?: false
            }

            if (matchingActivity != null) {
                matchingActivity.id?.let { dao.updateStravaId(session.id, it) }
                Log.i(TAG, "Activity already exists on Strava (id=${matchingActivity.id}), skipping upload.")
                NotificationHelper.showNotification(
                    ctx,
                    "Strava Sync Complete",
                    "Workout already uploaded on Strava: ${session.title}",
                    2
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
                session.avgHeartRate?.toFloat()
            )
            Log.i(TAG, "doWork: generated TCX file at ${tcxFile.absolutePath}")

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

            Log.i(TAG, "⬆️  uploaded file, got upload_id=$uploadId")
            Log.i(TAG, "Polling Strava for upload status, upload_id=$uploadId")

            // 2️⃣ poll until Strava finishes processing with exponential backoff
            val maxChecks = 60              // 60 × 4 s  ≈ 4 min
            var checks = 0
            var delayTime = 4000L // Start with 4 seconds
            val maxDelay = 60000L // Cap delay at 60 seconds
            var status: com.example.fitbodstravasyncer.data.strava.StravaUploadStatusResponse

            do {
                Log.i(TAG, "Polling attempt $checks for upload_id=$uploadId ...")
                kotlinx.coroutines.delay(delayTime)
                status = api.getUploadStatus(token, uploadId)
                Log.i(TAG, "Upload poll #$checks: status=${status.status}, activity_id=${status.activity_id}")
                delayTime = (delayTime * 2).coerceAtMost(maxDelay)
                checks++
            } while (status.activity_id == null && checks < maxChecks)

            if (status.activity_id != null) {
                dao.updateStravaId(sessionId, status.activity_id)
                Log.i(TAG, "doWork: upload complete, activity_id=${status.activity_id}")

                if (tcxFile.exists()) {
                    val deleted = tcxFile.delete()
                    Log.i(TAG, "Deleted TCX file: $deleted at ${tcxFile.absolutePath}")
                }

                NotificationHelper.showNotification(
                    ctx,
                    "Strava Sync Complete",
                    "Workout uploaded: ${session.title}",
                    2
                )
                return Result.success()
            } else {
                Log.e(TAG, "❌ Strava never returned an activity_id after $checks checks for upload_id=$uploadId")
                if (tcxFile.exists()) {
                    val deleted = tcxFile.delete()
                    Log.i(TAG, "Deleted TCX file after failed poll: $deleted at ${tcxFile.absolutePath}")
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
                Log.i(TAG, "Deleted TCX file on cancellation: $deleted at ${tcxFile.absolutePath}")
            }
            NotificationHelper.showNotification(
                ctx,
                "Strava Disconnected",
                "Please reconnect your Strava account to continue syncing.",
                1
            )
            Log.e(TAG, "doWork: cancelled", e)
            return Result.failure()
        } catch (e: Exception) {
            if (tcxFile != null && tcxFile.exists()) {
                val deleted = tcxFile.delete()
                Log.i(TAG, "Deleted TCX file on exception: $deleted at ${tcxFile.absolutePath}")
            }
            NotificationHelper.showNotification(
                ctx,
                "Strava Sync Failed",
                "Failed to upload workout. Will retry.",
                2
            )
            Log.e(TAG, "Upload failure", e)
            return Result.retry()
        }
    }

}
