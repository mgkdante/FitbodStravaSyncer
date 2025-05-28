package app.secondclass.healthsyncer.data.strava

import android.content.Context
import app.secondclass.healthsyncer.data.db.AppDatabase
import app.secondclass.healthsyncer.util.SessionMatcher
import app.secondclass.healthsyncer.util.safeStravaCall
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
