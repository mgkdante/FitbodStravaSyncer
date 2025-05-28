package app.secondclass.healthsyncer.util

import app.secondclass.healthsyncer.data.strava.StravaActivityResponse

object SessionMatcher {

    fun matchesSessionByTime(sessionEpoch: Long, activity: StravaActivityResponse, toleranceSeconds: Long = 300): Boolean {
        val actEpoch = try {
            activity.startDate?.let { java.time.Instant.parse(it).epochSecond }
        } catch (e: Exception) { null }
        return actEpoch != null && kotlin.math.abs(actEpoch - sessionEpoch) < toleranceSeconds
    }
}
