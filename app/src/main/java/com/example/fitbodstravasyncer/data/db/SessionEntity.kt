package com.example.fitbodstravasyncer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val dateTime: String,
    val startTime: Instant,
    val activeTime: Long,      // minutes
    val calories: Double,      // raw; worker rounds down
    val avgHeartRate: Long?, // raw; worker rounds down
    val stravaId: Long? = null // null = not yet uploaded
)
