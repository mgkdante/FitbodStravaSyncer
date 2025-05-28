package app.secondclass.healthsyncer.data.strava

import com.squareup.moshi.Json

data class StravaActivityResponse(
    val id: Long?,
    val name: String?,
    val description: String?,
    val type: String?,
    @Json(name = "sport_type") val sportType: String?,
    @Json(name = "start_date") val startDate: String?  // ISO8601 timestamp string
)
