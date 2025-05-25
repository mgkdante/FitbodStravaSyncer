package com.example.fitbodstravasyncer.data.strava

import com.squareup.moshi.Json

data class StravaActivityResponse(
    val id: Long?,
    val name: String?,
    @Json(name = "start_date") val startDate: String?  // ISO8601 timestamp string
)