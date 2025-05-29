package app.secondclass.healthsyncer.data.strava

import app.secondclass.healthsyncer.data.db.AppDatabase
import app.secondclass.healthsyncer.util.SessionMatcher
import app.secondclass.healthsyncer.util.safeStravaCall
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class StravaSyncHelper @Inject constructor(
    private val appDatabase: AppDatabase,
    private val stravaApiClient: StravaApiClient
) {
    suspend fun restoreStravaIds(
        toleranceSeconds: Long = 300,
        onRateLimit: (isAppLimit: Boolean) -> Unit = {},
        onUnauthorized: () -> Unit = {},
        onOtherError: (Throwable) -> Unit = {}
    ) {
        val dao = appDatabase.sessionDao()
        val sessions = dao.getAll().first()
        val activities = safeStravaCall(
            call = { stravaApiClient.listAllActivities() },
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
}
