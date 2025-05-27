package com.example.fitbodstravasyncer.data.strava

import android.content.Context
import com.example.fitbodstravasyncer.core.network.RetrofitProvider
import com.example.fitbodstravasyncer.util.StravaTokenManager
import okhttp3.MultipartBody
import okhttp3.RequestBody

class StravaApiClient(private val context: Context) {

    // ← unified API client via RetrofitProvider
    private val api: StravaActivityService by lazy {
        RetrofitProvider.createApiService(StravaActivityService::class.java)
    }

    /** Returns “Bearer <token>”, refreshing as needed. */
    private suspend fun getAuthToken(): String =
        "Bearer ${StravaTokenManager.getValidAccessToken(context)}"

    /**
     * Combines all pages into a single list.
     */
    suspend fun listAllActivities(perPage: Int = 200, after: Long? = null, before: Long? = null): List<StravaActivityResponse> {
        val token = getAuthToken()
        val all = mutableListOf<StravaActivityResponse>()
        var page = 1
        while (true) {
            val batch = api.listActivities(token, perPage, page, after, before)
            if (batch.isEmpty()) break
            all += batch
            page++
        }
        return all
    }


    /**
     * Helper: one page only.
     */
    suspend fun listActivitiesPage(
        page: Int,
        perPage: Int = 50,
        after: Long? = null,
        before: Long? = null
    ): List<StravaActivityResponse> {
        val token = getAuthToken()
        return api.listActivities(token, perPage, page, after, before)
    }


    /** Upload a single file (TCX, GPX, etc). */
    suspend fun uploadActivity(
        filePart: MultipartBody.Part,
        dataType: RequestBody,
        sportType: RequestBody,
        name: RequestBody,
        description: RequestBody
    ): StravaUploadResponse {
        val token = getAuthToken()
        return api.uploadActivity(
            auth        = token,
            file        = filePart,
            dataType    = dataType,
            sportType   = sportType,
            name        = name,
            description = description
        )
    }

    /** Poll an upload until it’s processed. */
    suspend fun getUploadStatus(
        uploadId: Long
    ): StravaUploadStatusResponse {
        val token = getAuthToken()
        return api.getUploadStatus(token, uploadId)
    }

    /** Fetch a single activity by its Strava ID. */
    suspend fun getActivity(
        activityId: Long
    ): StravaActivityResponse {
        val token = getAuthToken()
        return api.getActivity(token, activityId)
    }
}
