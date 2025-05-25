package com.example.fitbodstravasyncer.util

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.fitbodstravasyncer.data.db.SessionEntity
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object FitbodFetcher {

    suspend fun fetchFitbodSessions(
        healthClient: HealthConnectClient,
        startInstant: Instant,
        endInstant: Instant
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

            val id = "Workout*${formatter.format(record.startTime)}"

            val session = SessionEntity(
                id = id,
                title = "Fitbod Workout",
                description = record.title.orEmpty(),
                dateTime = "${formatter.format(record.startTime)} – ${formatter.format(record.endTime)}",
                startTime = record.startTime,
                activeTime = Duration.between(record.startTime, record.endTime).toMinutes(),
                calories = calories,
                avgHeartRate = heartRate?.toLong(),
                stravaId = null
            )
            sessions.add(session)
        }

        return sessions
    }
}
