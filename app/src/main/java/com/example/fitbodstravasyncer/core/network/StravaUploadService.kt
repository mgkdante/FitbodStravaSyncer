package com.example.fitbodstravasyncer.core.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

interface StravaUploadService {

    companion object {
        fun create(): StravaUploadService =
            Retrofit.Builder()
                .baseUrl("https://www.strava.com/")
                .client(OkHttpClient())
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(StravaUploadService::class.java)
    }
}

data class UploadResponse(
    val id: Long,
    val status: String,
    val external_id: String?,
    val error: String?
)

data class UploadStatusResponse(
    val id: Long,
    val status: String,
    val activityId: Long?
)
