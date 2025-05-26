package com.example.fitbodstravasyncer.util

import android.content.Context
import com.example.fitbodstravasyncer.data.db.HeartRateSampleEntity
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

object TcxFileGenerator {

    private val isoFormatter = DateTimeFormatter.ISO_INSTANT

    fun generateTcxFile(
        context: Context,
        sessionId: String,
        title: String,
        description: String,
        startTime: Instant,
        activeTimeSeconds: Float,
        calories: Float,
        avgHeartRate: Float?,
        heartRateSeries: List<HeartRateSampleEntity> = emptyList(),
        sportType: String = "Other"
    ): File {
        val fileName = sessionId.replace(Regex("[^A-Za-z0-9_]"), "_") + ".tcx"

        // ✨ Filter HR data within session window
        val endTime = startTime.plusSeconds(activeTimeSeconds.toLong())
        val filteredHeartRateSeries = heartRateSeries.filter { it.time in startTime..endTime }

        val xml = buildTcxXml(
            title, description, startTime, activeTimeSeconds, calories, avgHeartRate, sportType, filteredHeartRateSeries
        )
        val tcxFile = File(context.cacheDir, fileName)
        tcxFile.bufferedWriter(Charsets.UTF_8).use { it.write(xml) }
        return tcxFile
    }

    private fun buildTcxXml(
        title: String,
        description: String,
        startTime: Instant,
        activeTimeSeconds: Float,
        calories: Float,
        avgHeartRate: Float?,
        sportType: String,
        heartRateSeries: List<HeartRateSampleEntity> = emptyList()
    ): String {
        val startIso = isoFormatter.format(startTime)
        val endTime = startTime.plusSeconds(activeTimeSeconds.toLong())
        val endIso = isoFormatter.format(endTime)
        val totalTime = "%.1f".format(Locale.US, activeTimeSeconds)

        val trackPointsXml = if (heartRateSeries.isNotEmpty()) {
            heartRateSeries.joinToString("\n") { sample ->
                """
            <Trackpoint>
                <Time>${isoFormatter.format(sample.time)}</Time>
                <DistanceMeters>0.0</DistanceMeters>
                <HeartRateBpm><Value>${sample.bpm}</Value></HeartRateBpm>
            </Trackpoint>
            """.trimIndent()
            }
        } else {
            """
        <Trackpoint>
            <Time>$startIso</Time>
            <DistanceMeters>0.0</DistanceMeters>
            ${avgHeartRate?.let { "<HeartRateBpm><Value>${it.toInt()}</Value></HeartRateBpm>" } ?: ""}
        </Trackpoint>
        <Trackpoint>
            <Time>$endIso</Time>
            <DistanceMeters>0.0</DistanceMeters>
            ${avgHeartRate?.let { "<HeartRateBpm><Value>${it.toInt()}</Value></HeartRateBpm>" } ?: ""}
        </Trackpoint>
        """.trimIndent()
        }

        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <TrainingCenterDatabase
            xmlns="http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2
            http://www.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd">
          <Activities>
            <Activity Sport="$sportType">
              <Id>$startIso</Id>
              <Lap StartTime="$startIso">
                <TotalTimeSeconds>$totalTime</TotalTimeSeconds>
                <DistanceMeters>0.0</DistanceMeters>
                <Calories>${calories.toInt()}</Calories>
                ${avgHeartRate?.let { "<AverageHeartRateBpm><Value>${it.toInt()}</Value></AverageHeartRateBpm>" } ?: ""}
                <Intensity>Active</Intensity>
                <TriggerMethod>Manual</TriggerMethod>
                <Track>
                  $trackPointsXml
                </Track>
              </Lap>
              <Notes>${escapeXml(description)}</Notes>
            </Activity>
          </Activities>
        </TrainingCenterDatabase>
    """.trimIndent()
    }

    private fun escapeXml(str: String?): String {
        return str
            ?.replace("&", "&amp;")
            ?.replace("<", "&lt;")
            ?.replace(">", "&gt;")
            ?.replace("\"", "&quot;")
            ?.replace("'", "&apos;")
            ?: ""
    }
}
