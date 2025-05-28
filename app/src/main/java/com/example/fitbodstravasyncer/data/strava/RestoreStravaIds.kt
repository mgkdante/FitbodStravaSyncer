package com.example.fitbodstravasyncer.data.strava

import android.content.Context
import com.example.fitbodstravasyncer.data.db.AppDatabase
import com.example.fitbodstravasyncer.util.SessionMatcher
import com.example.fitbodstravasyncer.util.safeStravaCall
import kotlinx.coroutines.flow.first

suspend fun restoreStravaIds(
    ctx: Context,
    toleranceSeconds: Long = 300,
    onRateLimit: (isAppLimit: Boolean) -> Unit = {},
    onUnauthorized: () -> Unit = {},
    onOtherError: (Throwable) -> Unit = {}
) {
    val dao = AppDatabase.getInstance(ctx).sessionDao()
    val sessions = dao.getAll().first()

    val client = StravaApiClient(ctx)

    val activities = safeStravaCall(
        call = { client.listAllActivities() },
        onRateLimit = onRateLimit,
        onUnauthorized = onUnauthorized,
        onOtherError = onOtherError
    ) ?: return

    for (session in sessions) {
        val sessionEpoch = session.startTime.epochSecond
        val matching = activities.firstOrNull { activity ->
            SessionMatcher.matchesSessionByTime(sessionEpoch, activity, toleranceSeconds)
        }
        if (matching != null && (session.stravaId == null || session.stravaId != matching.id)) {
            matching.id?.let { stravaId -> dao.updateStravaId(session.id, stravaId) }
        }
    }
}
