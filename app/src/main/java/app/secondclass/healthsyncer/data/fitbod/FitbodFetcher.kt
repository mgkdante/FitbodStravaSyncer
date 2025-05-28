package app.secondclass.healthsyncer.data.fitbod

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import app.secondclass.healthsyncer.data.db.HeartRateSampleEntity
import app.secondclass.healthsyncer.data.db.SessionEntity
import app.secondclass.healthsyncer.data.strava.StravaActivityResponse
import app.secondclass.healthsyncer.data.strava.StravaApiClient
import app.secondclass.healthsyncer.util.SessionMatcher
import app.secondclass.healthsyncer.util.safeStravaCall
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object FitbodFetcher {

    suspend fun fetchFitbodSessionsWithStrava(
        context: Context,
        healthClient: HealthConnectClient,
        startInstant: Instant,
        endInstant: Instant,
        toleranceSeconds: Long = 300,
        onRateLimit: (isAppLimit: Boolean) -> Unit = {},
        onUnauthorized: () -> Unit = {},
        onOtherError: (Throwable) -> Unit = {}
    ): List<SessionEntity> {
        // Fetch Strava activities robustly (with error handling)
        val stravaActivities = safeStravaCall(
            call = {
                val client = StravaApiClient(context)
                client.listAllActivities()
            },
            onRateLimit = onRateLimit,
            onUnauthorized = onUnauthorized,
            onOtherError = onOtherError
        ) ?: return emptyList()

        // Then fetch Fitbod sessions and match as before
        return fetchFitbodSessions(
            healthClient = healthClient,
            startInstant = startInstant,
            endInstant = endInstant,
            stravaActivities = stravaActivities,
            toleranceSeconds = toleranceSeconds
        )
    }

    suspend fun fetchFitbodSessions(
        healthClient: HealthConnectClient,
        startInstant: Instant,
        endInstant: Instant,
        stravaActivities: List<StravaActivityResponse>,
        toleranceSeconds: Long = 300
    ): List<SessionEntity> {
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a")
            .withZone(ZoneId.systemDefault())

        val response = healthClient.readRecords(
            ReadRecordsRequest(
                ExerciseSessionRecord::class,
                TimeRangeFilter.Companion.between(startInstant, endInstant)
            )
        )

        val sessions = mutableListOf<SessionEntity>()

        for (record in response.records) {
            if (!record.metadata.dataOrigin.packageName.contains("fitbod")) continue

            val calories = healthClient.aggregate(
                AggregateRequest(
                    setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                    TimeRangeFilter.Companion.between(record.startTime, record.endTime)
                )
            )[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0

            val heartRate = healthClient.aggregate(
                AggregateRequest(
                    setOf(HeartRateRecord.BPM_AVG),
                    TimeRangeFilter.Companion.between(record.startTime, record.endTime)
                )
            )[HeartRateRecord.BPM_AVG]?.toDouble()

            val heartRateRecords = healthClient.readRecords(
                ReadRecordsRequest(
                    HeartRateRecord::class,
                    TimeRangeFilter.Companion.between(record.startTime, record.endTime)
                )
            ).records

            // Filter HR samples within session only
            val sessionEnd = record.endTime
            val heartRateSeries = heartRateRecords.flatMap { hrRec ->
                hrRec.samples.map { sample ->
                    HeartRateSampleEntity(
                        time = sample.time,
                        bpm = sample.beatsPerMinute.toLong()
                    )
                }
            }.filter { it.time >= record.startTime && it.time <= sessionEnd }
                .sortedBy { it.time }

            val id = "Workout*${formatter.format(record.startTime)}"
            val sessionEpoch = record.startTime.epochSecond

            // --- Strava match logic: match by start time within tolerance (DRY)
            val match = stravaActivities.firstOrNull { activity ->
                SessionMatcher.matchesSessionByTime(sessionEpoch, activity, toleranceSeconds)
            }
            val stravaId = match?.id

            val session = SessionEntity(
                id = id,
                title = "Fitbod Workout: ${record.title}",
                description = record.notes.orEmpty(),
                dateTime = "${formatter.format(record.startTime)} – ${formatter.format(record.endTime)}",
                startTime = record.startTime,
                activeTime = Duration.between(record.startTime, record.endTime).toMinutes(),
                calories = calories,
                avgHeartRate = heartRate?.toLong(),
                heartRateSeries = heartRateSeries,
                stravaId = stravaId
            )
            sessions.add(session)
        }

        return sessions
    }
}
