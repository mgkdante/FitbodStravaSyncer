package com.example.fitbodstravasyncer.util

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.fitbodstravasyncer.data.db.HeartRateSampleEntity
import com.example.fitbodstravasyncer.data.db.SessionEntity
import com.example.fitbodstravasyncer.data.strava.StravaActivityResponse
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

object FitbodFetcher {

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
                TimeRangeFilter.between(startInstant, endInstant)
            )
        )

        val sessions = mutableListOf<SessionEntity>()

        for (record in response.records) {
            if (!record.metadata.dataOrigin.packageName.contains("fitbod")) continue

            val calories = healthClient.aggregate(
                AggregateRequest(
                    setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                    TimeRangeFilter.between(record.startTime, record.endTime)
                )
            )[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0

            val heartRate = healthClient.aggregate(
                AggregateRequest(
                    setOf(HeartRateRecord.BPM_AVG),
                    TimeRangeFilter.between(record.startTime, record.endTime)
                )
            )[HeartRateRecord.BPM_AVG]?.toDouble()

            val heartRateRecords = healthClient.readRecords(
                ReadRecordsRequest(
                    HeartRateRecord::class,
                    TimeRangeFilter.between(record.startTime, record.endTime)
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

            // --- Strava match logic: match by start time within tolerance
            val match = stravaActivities.firstOrNull { activity ->
                activity.startDate?.let {
                    val actEpoch = try { Instant.parse(it).epochSecond } catch (e: Exception) { null }
                    actEpoch != null && abs(actEpoch - sessionEpoch) < toleranceSeconds
                } == true
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
