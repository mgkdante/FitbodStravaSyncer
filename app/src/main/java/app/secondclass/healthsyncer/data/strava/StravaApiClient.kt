package app.secondclass.healthsyncer.data.strava

import android.content.Context
import app.secondclass.healthsyncer.data.strava.StravaConstants.PER_PAGE
import app.secondclass.healthsyncer.util.StravaPrefs
import app.secondclass.healthsyncer.util.StravaTokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StravaApiClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: StravaActivityService,
    private val stravaTokenManager: StravaTokenManager
) {

    /** Returns “Bearer <token>”, refreshing as needed. */
    private suspend fun getAuthToken(): String =
        "Bearer ${stravaTokenManager.getValidAccessToken(context)}"

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
            if (batch.isEmpty()) break
            StravaPrefs.incrementUserApiRequest(context, isRead = true)
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
