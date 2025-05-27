package com.example.fitbodstravasyncer.data.strava

import android.content.Context
import com.example.fitbodstravasyncer.data.db.AppDatabase
import kotlinx.coroutines.flow.first


suspend fun restoreStravaIds(ctx: Context, toleranceSeconds: Long = 300) {
    val dao = AppDatabase.getInstance(ctx).sessionDao()
    val sessions = dao.getAll().first()

    val client = StravaApiClient(ctx)
    val activities = client.listAllActivities()

    for (session in sessions) {
        val sessionEpoch = session.startTime.epochSecond
        val matching = activities.firstOrNull { activity ->
            activity.startDate?.let {
                val actEpoch = try { java.time.Instant.parse(it).epochSecond } catch (e: Exception) { null }
                actEpoch != null && kotlin.math.abs(actEpoch - sessionEpoch) < toleranceSeconds
            } == true
        }
        if (matching != null && (session.stravaId == null || session.stravaId != matching.id)) {
            matching.id?.let { stravaId -> dao.updateStravaId(session.id, stravaId) }
        }
    }
}
