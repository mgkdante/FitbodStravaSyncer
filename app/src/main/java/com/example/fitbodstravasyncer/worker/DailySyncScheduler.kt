package com.example.fitbodstravasyncer.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object DailySyncScheduler {
    private const val UNIQUE_WORK_NAME = "daily_strava_sync"

    fun schedule(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<StravaDailySyncWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.Companion.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun cancel(context: Context) {
        WorkManager.Companion.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}