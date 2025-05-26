package com.example.fitbodstravasyncer.data.db

import androidx.room.TypeConverter
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.Instant

class Converters {

    companion object {
        private val moshi: Moshi = Moshi.Builder()
            .add(InstantJsonAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()
        private val type = Types.newParameterizedType(List::class.java, HeartRateSampleEntity::class.java)
        private val adapter: JsonAdapter<List<HeartRateSampleEntity>> = moshi.adapter(type)
    }

    @TypeConverter
    fun fromHeartRateList(list: List<HeartRateSampleEntity>?): String? =
        list?.let { adapter.toJson(it) }

    @TypeConverter
    fun toHeartRateList(json: String?): List<HeartRateSampleEntity> =
        json?.let { adapter.fromJson(it) ?: emptyList() } ?: emptyList()

    // Store Instant as ISO-8601 String for safety
    @TypeConverter
    fun fromInstant(value: Instant?): String? = value?.toString()

    @TypeConverter
    fun toInstant(value: String?): Instant? = value?.let { Instant.parse(it) }
}

class InstantJsonAdapter {
    @ToJson
    fun toJson(value: Instant): String = value.toString()

    @FromJson
    fun fromJson(value: String): Instant = Instant.parse(value)
}
