package app.secondclass.healthsyncer.data.strava

import android.content.Context
import app.secondclass.healthsyncer.core.network.RetrofitProvider
import app.secondclass.healthsyncer.data.strava.StravaConstants.PER_PAGE
import app.secondclass.healthsyncer.util.StravaPrefs
import app.secondclass.healthsyncer.util.StravaTokenManager
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
    suspend fun listAllActivities(
        perPage: Int = PER_PAGE,
        after: Long? = null,
        before: Long? = null,
        cacheLastFetch: Boolean = false
    ): List<StravaActivityResponse> {
        StravaPrefs.incrementUserApiRequest(context, isRead = true)
        val token = getAuthToken()
        val all = mutableListOf<StravaActivityResponse>()
        var page = 1
        while (true) {
            val batch = api.listActivities(token, perPage, page, after, before)
            StravaPrefs.incrementUserApiRequest(context, isRead = true)
            if (batch.isEmpty()) break
            all += batch
            page++
        }

        // Update last fetch epoch
        if (cacheLastFetch && all.isNotEmpty()) {
            val maxEpoch = all.mapNotNull { it.startDate }
                .mapNotNull { runCatching { java.time.Instant.parse(it).epochSecond }.getOrNull() }
                .maxOrNull()
            if (maxEpoch != null) {
                StravaPrefs.setLastFetchEpoch(context, maxEpoch)
            }
        }
        return all
    }

    /**
     * Helper: one page only.
     */

    /** Upload a single file (TCX, GPX, etc). */
    suspend fun uploadActivity(
        filePart: MultipartBody.Part,
        dataType: RequestBody,
        sportType: RequestBody,
        name: RequestBody,
        description: RequestBody
    ): StravaUploadResponse {
        StravaPrefs.incrementUserApiRequest(context, isRead = false)
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
        StravaPrefs.incrementUserApiRequest(context, isRead = true)
        val token = getAuthToken()
        return api.getUploadStatus(token, uploadId)
    }

}
