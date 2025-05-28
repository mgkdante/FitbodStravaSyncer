package app.secondclass.healthsyncer.auth

import android.net.Uri
import app.secondclass.healthsyncer.data.strava.StravaConstants
import androidx.core.net.toUri

object StravaAuthManager {
    fun buildAuthUri(): Uri {
        return StravaConstants.AUTH_URL.toUri().buildUpon()
            .appendQueryParameter("client_id", StravaConstants.CLIENT_ID)
            .appendQueryParameter("redirect_uri", StravaConstants.REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", "activity:read_all,activity:write")
            .appendQueryParameter("approval_prompt", "auto")
            .build()
    }
}
