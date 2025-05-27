package com.example.fitbodstravasyncer.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fitbodstravasyncer.data.strava.restoreStravaIds
import com.example.fitbodstravasyncer.util.NotificationHelper

class StravaDailySyncWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            restoreStravaIds(applicationContext)
            // Notify the user when the daily sync is done
            NotificationHelper.showNotification(
                applicationContext,
                "Daily Strava Sync",
                "Your Fitbod sessions have been checked against Strava.",
                10124 // Notification ID; pick any unique int
            )
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
