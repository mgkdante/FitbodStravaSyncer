package app.secondclass.healthsyncer.worker

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.*
import app.secondclass.healthsyncer.data.db.AppDatabase
import app.secondclass.healthsyncer.data.strava.StravaApiClient
import app.secondclass.healthsyncer.data.strava.StravaConstants.PER_PAGE
import app.secondclass.healthsyncer.data.strava.StravaUploadStatusResponse
import app.secondclass.healthsyncer.util.NotificationHelper
import app.secondclass.healthsyncer.util.StravaPrefs
import app.secondclass.healthsyncer.util.TcxFileGenerator
import app.secondclass.healthsyncer.util.isStravaConnected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
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
                Toast.makeText(context, UiStrings.INVALID_SESSION_FOR_SYNC, Toast.LENGTH_SHORT).show()
                return
            }
            if (!context.isStravaConnected()) {
                Toast.makeText(context, UiStrings.CONNECT_STRAVA_FIRST, Toast.LENGTH_SHORT).show()
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
        val openAppIntent = NotificationHelper.createOpenAppIntent(applicationContext)

        // ==== CIRCUIT BREAKER: Prevent uploads if tripped ====
        if (StravaPrefs.isUploadCircuitBreakerTripped(ctx)) {
            NotificationHelper.showNotification(
                ctx,
                UiStrings.STRAVA_DISCONNECTED_TITLE,
                "You were logged out of Strava after repeated errors. Tap 'Reconnect' in Settings.",
                10199,
                openAppIntent
            )
            return@withContext Result.failure()
        }

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
                UiStrings.SYNCING_TO_STRAVA_TITLE,
                "Uploading workout: ${session.title}…",
                notificationId,
                openAppIntent
            )

            // Already locally marked?
            if (session.stravaId != null) {
                NotificationHelper.showNotification(
                    ctx, UiStrings.STRAVA_SYNC_COMPLETE_TITLE,
                    "Workout already uploaded: ${session.title}",
                    notificationId,
                    openAppIntent
                )
                StravaPrefs.resetUploadFailureCount(ctx) // reset breaker on success
                return@withContext Result.success()
            }

            // --- DRY remote-check via StravaApiClient ---
            val recentActivities = client.listAllActivities(perPage = PER_PAGE)
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
                    ctx, UiStrings.STRAVA_SYNC_COMPLETE_TITLE,
                    "Workout already on Strava: ${session.title}",
                    notificationId,
                    openAppIntent
                )
                StravaPrefs.resetUploadFailureCount(ctx) // reset breaker on success
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
                    ctx, UiStrings.STRAVA_SYNC_COMPLETE_TITLE,
                    "Workout uploaded: ${session.title}",
                    notificationId,
                    openAppIntent
                )
                StravaPrefs.resetUploadFailureCount(ctx) // reset breaker on success
                return@withContext Result.success()
            } else {
                tcxFile.delete()
                handleBreakerAndLogoutIfNeeded("Failed to upload workout: ${session.title}. Will retry.")
                return@withContext Result.retry()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            tcxFile?.delete()
            handleBreakerAndLogoutIfNeeded(UiStrings.STRAVA_DISCONNECTED_BODY)
            return@withContext Result.failure()
        } catch (e: HttpException) {
            val code = e.code()
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            val msg = when (code) {
                429 -> {
                    val now = System.currentTimeMillis()
                    val min15Start = now / (15 * 60 * 1000)
                    val next15Reset = ((min15Start + 1) * 15 * 60 * 1000)
                    StravaPrefs.setApiLimitReset(ctx, next15Reset)
                    "API rate limit hit. Try again in ${(next15Reset - now) / 60000}m."
                }
                400 -> "Upload failed: ${parseErrorMessage(errorBody) ?: "Bad file or data"}"
                401 -> UiStrings.TOKEN_INVALID_EXPIRED
                else -> "Upload failed (${code}): ${parseErrorMessage(errorBody) ?: errorBody}"
            }
            val isBreakerCase = (code == 401 || code == 403 || code == 429)
            handleBreakerAndLogoutIfNeeded(msg, isBreakerCase)
            return@withContext if (code == 429) Result.retry() else Result.failure()
        } catch (e: Exception) {
            tcxFile?.delete()
            handleBreakerAndLogoutIfNeeded(UiStrings.GENERIC_UPLOAD_FAILED)
            return@withContext Result.retry()
        }
    }

    // Helper to increment breaker and log out if needed
    private fun handleBreakerAndLogoutIfNeeded(notificationMsg: String, alwaysIncrement: Boolean = true) {
        val openAppIntent = NotificationHelper.createOpenAppIntent(applicationContext)
        if (StravaPrefs.shouldShowErrorNotification(ctx)) {
            NotificationHelper.showNotification(
                ctx,
                UiStrings.STRAVA_SYNC_FAILED_TITLE,
                notificationMsg,
                2,
                openAppIntent
            )
            StravaPrefs.markErrorNotificationShown(ctx)
        }
        if (alwaysIncrement) StravaPrefs.incrementUploadFailureCount(ctx)
        if (StravaPrefs.getUploadFailureCount(ctx) >= 3) {
            // LOG OUT user and trip breaker
            StravaPrefs.disconnect(ctx)
            StravaPrefs.setUploadCircuitBreaker(ctx, true)
            NotificationHelper.showNotification(
                ctx,
                UiStrings.STRAVA_DISCONNECTED_TITLE,
                "You were logged out of Strava after repeated errors. Tap 'Reconnect' in Settings.",
                10200,
                openAppIntent
            )
        }
    }
}

// (You can move this to a utils file if you want)
private fun parseErrorMessage(errorBody: String): String? {
    // Strava error bodies are often JSON like: {"message":"Some error","errors":[...]}"
    return Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(errorBody)?.groupValues?.get(1)
}
