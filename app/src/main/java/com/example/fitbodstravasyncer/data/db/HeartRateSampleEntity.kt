package com.example.fitbodstravasyncer.data.db
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
data class HeartRateSampleEntity(
    val time: Instant,
    val bpm: Long
)
