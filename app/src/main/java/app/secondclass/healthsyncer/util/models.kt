package app.secondclass.healthsyncer.util

import androidx.compose.runtime.Composable
import app.secondclass.healthsyncer.data.db.HeartRateSampleEntity
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
    val dynamicColor: Boolean = true,
    val apiRequestsDay: Int = 0,
    val apiRequests15Min: Int = 0,
    val apiLimitReached: Boolean = false,
    val apiLimitResetHint: String = "",
    val hasFetchedOnce: Boolean = false,
    val userApiUsageString: String = "",
    val userApiWarning: Boolean = false
)

data class LabeledControlConfig(
    val key: String, // unique
    val helpTitle: String,
    val helpDescription: String,
    val label: String? = null,
    val content: @Composable () -> Unit
)
