package com.example.fitbodstravasyncer.worker

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.*
import com.example.fitbodstravasyncer.data.db.AppDatabase
import com.example.fitbodstravasyncer.data.strava.StravaApiClient
import com.example.fitbodstravasyncer.data.strava.StravaUploadStatusResponse
import com.example.fitbodstravasyncer.util.NotificationHelper
import com.example.fitbodstravasyncer.util.TcxFileGenerator
import com.example.fitbodstravasyncer.util.isStravaConnected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class StravaUploadWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    private val ctx = applicationContext

    companion object {
        private const val TAG = "STRAVA-sync"

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
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<StravaUploadWorker>()
                    .setInputData(inputData)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build()
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        var tcxFile: File? = null
        val client = StravaApiClient(ctx)
        try {
            val sessionId = inputData.getString("SESSION_ID")?.takeIf { it.isNotBlank() }
                ?: run {
                    Log.e(TAG, "doWork: SESSION_ID missing!")
                    return@withContext Result.failure()
                }

            val dao     = AppDatabase.getInstance(ctx).sessionDao()
            val session = dao.getById(sessionId) ?: run {
                Log.e(TAG, "doWork: session not found for id=$sessionId")
                return@withContext Result.failure()
            }

            val notificationId = session.id.hashCode()
            NotificationHelper.showNotification(
                ctx,
                "Syncing to Strava",
                "Uploading workout: ${session.title}…",
                notificationId
            )

            // Already locally marked?
            if (session.stravaId != null) {
                NotificationHelper.showNotification(
                    ctx, "Strava Sync Complete",
                    "Workout already uploaded: ${session.title}",
                    notificationId
                )
                return@withContext Result.success()
            }

            // --- DRY remote-check via StravaApiClient ---
            val recentActivities = client.listAllActivities(perPage = 200)
            val formatter        = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)
            val sessionEpoch     = session.startTime.epochSecond
            val tolerance        = 300L // 5 min

            val matching = recentActivities.firstOrNull { activity ->
                activity.startDate?.let {
                    val actEpoch = Instant.from(formatter.parse(it)).epochSecond
                    abs(actEpoch - sessionEpoch) < tolerance
                } == true
            }

            if (matching != null) {
                matching.id?.let { dao.updateStravaId(session.id, it) }
                NotificationHelper.showNotification(
                    ctx, "Strava Sync Complete",
                    "Workout already on Strava: ${session.title}",
                    notificationId
                )
                return@withContext Result.success()
            }

            // build & upload TCX…
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

            // --- DRY upload through StravaApiClient (NO manual token) ---
            val response = client.uploadActivity(
                MultipartBody.Part.createFormData("file", "${session.id}.tcx", tcxFile.asRequestBody("application/xml".toMediaType())),
                "tcx".toRequestBody(),
                "WeightTraining".toRequestBody(),
                session.title.toRequestBody(),
                session.description.toRequestBody()
            )

            // Poll status
            var delayMs = 4_000L
            var checks  = 0
            val maxChecks = 60
            var status: StravaUploadStatusResponse

            do {
                delay(delayMs)
                status = client.getUploadStatus(response.id)
                delayMs = (delayMs * 2).coerceAtMost(60_000L)
                checks++
            } while (status.activity_id == null && checks < maxChecks)

            if (status.activity_id != null) {
                dao.updateStravaId(sessionId, status.activity_id)
                tcxFile.delete()
                NotificationHelper.showNotification(
                    ctx, "Strava Sync Complete",
                    "Workout uploaded: ${session.title}",
                    notificationId
                )
                Result.success()
            } else {
                tcxFile.delete()
                NotificationHelper.showNotification(
                    ctx, "Strava Sync Failed",
                    "Failed to upload workout: ${session.title}. Will retry.",
                    2
                )
                Result.retry()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            tcxFile?.delete()
            NotificationHelper.showNotification(
                ctx, "Strava Disconnected",
                "Please reconnect your Strava account to continue syncing.",
                1
            )
            Result.failure()
        } catch (e: Exception) {
            tcxFile?.delete()
            NotificationHelper.showNotification(
                ctx, "Strava Sync Failed",
                "Failed to upload workout. Will retry.",
                2
            )
            Result.retry()
        }
    }

}
