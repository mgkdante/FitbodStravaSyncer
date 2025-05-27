package com.example.fitbodstravasyncer.data.strava

import android.content.Context
import com.example.fitbodstravasyncer.core.network.RetrofitProvider
import com.example.fitbodstravasyncer.data.db.AppDatabase
import com.example.fitbodstravasyncer.util.StravaTokenManager

import kotlinx.coroutines.flow.first // Add this import!

suspend fun restoreStravaIds(ctx: Context, toleranceSeconds: Long = 300) {
    val token = "Bearer ${StravaTokenManager.getValidAccessToken(ctx)}"
    val api = RetrofitProvider.retrofit.create(StravaActivityService::class.java)
    val dao = AppDatabase.getInstance(ctx).sessionDao()

    val sessions = dao.getAll().first() // <-- THIS GETS THE CURRENT LIST
    var page = 1
    val perPage = 200
    var activities: List<StravaActivityResponse>
    do {
        activities = api.listActivities(token, perPage, page)
        if (activities.isEmpty()) break

        for (session in sessions) {
            val sessionEpoch = session.startTime.epochSecond
            val matching = activities.firstOrNull { activity ->
                activity.startDate?.let {
                    val actEpoch = java.time.Instant.parse(it).epochSecond
                    kotlin.math.abs(actEpoch - sessionEpoch) < toleranceSeconds
                } == true
            }
            if (matching != null && (session.stravaId == null || session.stravaId != matching.id)) {
                matching.id?.let { stravaId ->
                    dao.updateStravaId(session.id, stravaId)
                }
            }
        }
        page++
    } while (activities.isNotEmpty())
}
