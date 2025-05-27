package com.example.fitbodstravasyncer.util

import com.example.fitbodstravasyncer.data.db.HeartRateSampleEntity
import java.time.Instant
import java.time.LocalDate

data class SessionMetrics(
    val id: String,
    val title: String,
    val description: String,
    val dateTime: String,
    val startTime: Instant,
    val activeTime: Long,
    val calories: Double,
    val avgHeartRate: Double?,
    val heartRateSeries: List<HeartRateSampleEntity> = emptyList(),
    val stravaId: Long?
)


data class UiState(
    val sessionMetrics: List<SessionMetrics> = emptyList(),
    val futureSync: Boolean = false,
    val dailySync: Boolean = false,
    val stravaConnected: Boolean = false,
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
    val isFetching: Boolean = false,
    val dynamicColor: Boolean = true

)

