package com.example.fitbodstravasyncer.data.strava

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

data class StravaUploadResponse(
    val id: Long,
    val external_id: String?,
    val error: String?
)

data class StravaUploadStatusResponse(
    val id: Long,
    val status: String,
    val activity_id: Long?
)

interface StravaActivityService {
    @Multipart
    @POST("uploads")
    suspend fun uploadActivity(
        @Header("Authorization") auth: String,
        @Part file: MultipartBody.Part,
        @Part("data_type") dataType: RequestBody,
        @Part("sport_type") sportType: RequestBody,
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody
    ): StravaUploadResponse


    @GET("uploads/{uploadId}")
    suspend fun getUploadStatus(
        @Header("Authorization") auth: String,
        @Path("uploadId") uploadId: Long
    ): StravaUploadStatusResponse


    @GET("activities")
    suspend fun listActivities(
        @Header("Authorization") auth: String,
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1,
        @Query("after") after: Long? = null,
        @Query("before") before: Long? = null
    ): List<StravaActivityResponse>


    @GET("activities/{id}")
    suspend fun getActivity(
        @Header("Authorization") auth: String,
        @Path("id") activityId: Long
    ): StravaActivityResponse


}
