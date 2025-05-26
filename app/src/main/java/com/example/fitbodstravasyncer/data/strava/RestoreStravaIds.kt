package com.example.fitbodstravasyncer.data.strava

import android.content.Context
import android.util.Log
import com.example.fitbodstravasyncer.core.network.RetrofitProvider
import com.example.fitbodstravasyncer.data.db.AppDatabase
import com.example.fitbodstravasyncer.util.StravaPrefs
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import retrofit2.HttpException
import androidx.core.content.edit
import com.example.fitbodstravasyncer.util.StravaTokenManager

private const val KEY_LAST_429 = "last_429_timestamp"
private const val RATE_LIMIT_BACKOFF_MS = 15 * 60 * 1000L

suspend fun restoreStravaIds(
    ctx: Context,
    toleranceSeconds: Long = 300,
    since: Instant? = null
) {
    val prefs = StravaPrefs.securePrefs(ctx)
    val last429 = prefs.getLong(KEY_LAST_429, 0L)
    val now = System.currentTimeMillis()
    if (now - last429 < RATE_LIMIT_BACKOFF_MS) {
        Log.w("restoreStravaIds", "Skipping restore: last 429 was ${((now - last429) / 1000 / 60)} minutes ago.")
        return
    }

    try {
        val token = "Bearer ${StravaTokenManager.getValidAccessToken(ctx)}"
        val api = RetrofitProvider.retrofit.create(StravaActivityService::class.java)
        val dao = AppDatabase.getInstance(ctx).sessionDao()
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)

        var page = 1
        val perPage = 200
        var activities: List<StravaActivityResponse> = emptyList()

        do {
            try {
                activities = api.listActivities(token, perPage, page)
                Log.d("StravaSync", "Page $page: Strava activities: $activities")
            } catch (e: HttpException) {
                if (e.code() == 429) {
                    prefs.edit { putLong(KEY_LAST_429, System.currentTimeMillis()) }
                    Log.w("restoreStravaIds", "Rate limit hit, aborting restore and backing off for 15 minutes.")
                    return
                } else {
                    Log.e("restoreStravaIds", "HTTP error: ${e.code()} - ${e.message()}", e)
                    return
                }
            }

            if (activities.isEmpty()) break

            for (activity in activities) {
                val isFitbod = activity.name?.contains("fitbod", ignoreCase = true) == true ||
                        activity.description?.contains("fitbod", ignoreCase = true) == true
                val isWeightTraining = activity.type == "WeightTraining" || activity.sportType == "WeightTraining"
                if (!(isFitbod || isWeightTraining)) continue

                val activityStartInstant = Instant.from(formatter.parse(activity.startDate))
                val activityEpochSec = activityStartInstant.epochSecond

                // Match session by time (with tolerance)
                val match = dao.findByStartTimeWithTolerance(activityEpochSec, toleranceSeconds)
                if (match != null && match.stravaId == null) {
                    activity.id?.let { stravaId ->
                        Log.i("restoreStravaIds", "Updating session ${match.id} with Strava ID $stravaId (matched on $activityEpochSec)")
                        dao.updateStravaId(match.id, stravaId)
                    }
                } else if (match != null) {
                    Log.i("restoreStravaIds", "Session ${match.id} already has Strava ID: ${match.stravaId}")
                }
            }

            page++
        } while (activities.isNotEmpty())

    } catch (e: HttpException) {
        Log.e("restoreStravaIds", "HTTP error outside paging: ${e.code()} - ${e.message()}", e)
    } catch (e: Exception) {
        Log.e("restoreStravaIds", "Unexpected error: ${e.localizedMessage}", e)
    }
}
