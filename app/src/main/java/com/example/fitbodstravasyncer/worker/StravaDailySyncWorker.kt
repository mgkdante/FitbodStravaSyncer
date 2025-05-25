package com.example.fitbodstravasyncer.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fitbodstravasyncer.data.strava.restoreStravaIds

class StravaDailySyncWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            restoreStravaIds(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
