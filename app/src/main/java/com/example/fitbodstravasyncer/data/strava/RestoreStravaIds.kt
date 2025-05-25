package com.example.fitbodstravasyncer.data.strava

import android.content.Context
import android.util.Log
import com.example.fitbodstravasyncer.core.network.RetrofitProvider
import com.example.fitbodstravasyncer.data.db.AppDatabase
import com.example.fitbodstravasyncer.util.StravaPrefs
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset

import kotlinx.coroutines.delay
import retrofit2.HttpException
import androidx.core.content.edit
import com.example.fitbodstravasyncer.util.StravaTokenManager

suspend fun restoreStravaIds(ctx: Context, toleranceSeconds: Long = 300, since: Instant? = null) {
    val token = "Bearer ${StravaTokenManager.getValidAccessToken(ctx)}"
    val api = RetrofitProvider.retrofit.create(StravaActivityService::class.java)
    val dao = AppDatabase.getInstance(ctx).sessionDao()
    val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)

    var page = 1
    val perPage = 200
    var activities: List<StravaActivityResponse> = emptyList()

    val prefs = StravaPrefs.securePrefs(ctx)
    val lastSyncEpochSec = prefs.getLong(StravaPrefs.KEY_LAST_SYNCED_ACTIVITY_TIME, 0L)

    var reachedOldActivities = false

    do {
        try {
            activities = api.listActivities(token, perPage, page)
        } catch (e: HttpException) {
            if (e.code() == 429) {
                Log.w("restoreStravaIds", "Rate limit hit, delaying 1 minute before retrying page $page")
                delay(60_000)
                continue
            } else {
                throw e
            }
        }

        if (activities.isEmpty()) break

        for (activity in activities) {
            val activityStartInstant = Instant.from(formatter.parse(activity.startDate))
            val activityEpochSec = activityStartInstant.epochSecond

            // If activity is older or equal to last synced, mark flag to break paging
            if (activityEpochSec <= lastSyncEpochSec) {
                reachedOldActivities = true
                break
            }

            // Match local session and update stravaId as before
            val match = dao.findByStartTimeWithTolerance(activityEpochSec, toleranceSeconds)
            if (match != null && match.stravaId == null) {
                activity.id?.let { stravaId ->
                    dao.updateStravaId(match.id, stravaId)
                }
            }
        }
        if (reachedOldActivities) break

        // Update last synced activity time with max activity timestamp on this page
        val maxActivityEpochSec = activities.maxOf {
            Instant.from(formatter.parse(it.startDate)).epochSecond
        }
        prefs.edit { putLong(StravaPrefs.KEY_LAST_SYNCED_ACTIVITY_TIME, maxActivityEpochSec) }

        page++
    } while (activities.isNotEmpty())


}
