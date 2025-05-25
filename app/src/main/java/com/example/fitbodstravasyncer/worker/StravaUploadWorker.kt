// File: StravaUploadWorker.kt
package com.example.fitbodstravasyncer.worker

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.*
import com.example.fitbodstravasyncer.core.network.RetrofitProvider
import com.example.fitbodstravasyncer.data.db.AppDatabase
import com.example.fitbodstravasyncer.data.strava.StravaActivityService
import com.example.fitbodstravasyncer.data.strava.StravaUploadStatusResponse
import com.example.fitbodstravasyncer.util.TcxFileGenerator
import androidx.work.workDataOf
import com.example.fitbodstravasyncer.data.strava.StravaUploadResponse
import com.example.fitbodstravasyncer.util.StravaTokenManager
import com.example.fitbodstravasyncer.util.isStravaConnected
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
        fun enqueue(context: Context, sessionId: String) {
            if (!context.isStravaConnected()) {
                Toast.makeText(context, "Connect Strava first", Toast.LENGTH_SHORT).show()
                return
            }
            val inputData = workDataOf("SESSION_ID" to sessionId)

            WorkManager.getInstance(context).enqueueUniqueWork(
                "UPLOAD_$sessionId",
                ExistingWorkPolicy.REPLACE,
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
        return try {
            Log.i(TAG, "doWork: started")

            val sessionId = inputData.getString("SESSION_ID") ?: run {
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

            if (session.stravaId != null) {
                Log.i(TAG, "doWork: already uploaded, stravaId=${session.stravaId}")
                return Result.success()
            }

            // ------- build TCX (summary only) -------
            val tcxFile = TcxFileGenerator.generateTcxFile(
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

            // ------- hit Strava -------
            val token = "Bearer ${StravaTokenManager.getValidAccessToken(ctx)}"
            Log.i(TAG, "doWork: got token")

            val api = RetrofitProvider.retrofit.create(StravaActivityService::class.java)
            Log.i(TAG, "doWork: created API")

            // 1️⃣ upload
            val dataType: RequestBody = "tcx".toRequestBody()
            val sportType: RequestBody = "WeightTraining".toRequestBody()
            val name: RequestBody = session.title.toRequestBody()
            val description: RequestBody = session.description.toRequestBody()

            val response: StravaUploadResponse = api.uploadActivity(
                token,
                MultipartBody.Part.createFormData(
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

            // 2️⃣ poll until Strava finishes processing
            val maxChecks = 60              // 60 × 4 s  ≈ 4 min
            var checks = 0
            var status: StravaUploadStatusResponse

            do {
                delay(4000)
                status = api.getUploadStatus(token, uploadId)
                Log.d(TAG, "poll … ${status.status}")
                checks++
            } while (status.activity_id == null && checks < maxChecks)

            if (status.activity_id != null) {
                dao.updateStravaId(sessionId, status.activity_id)
                Log.i(TAG, "doWork: upload complete, activity_id=${status.activity_id}")

                if (tcxFile.exists()) {
                    val deleted = tcxFile.delete()
                    Log.i(TAG, "Deleted TCX file: $deleted at ${tcxFile.absolutePath}")
                }

                Result.success()
            } else {
                Log.e(TAG, "❌ Strava never returned an activity_id after $checks checks")
                Result.retry()
            }
        } catch (_: CancellationException) {
            Log.e(TAG, "doWork: cancelled")
            Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Upload failure", e)
            Result.retry()
        }
    }
}

