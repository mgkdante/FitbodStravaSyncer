package com.example.fitbodstravasyncer.auth

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.example.fitbodstravasyncer.data.strava.StravaConstants
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

    fun launchStravaAuth(context: Context, customTabsIntent: CustomTabsIntent) {
        val authUri = buildAuthUri()
        customTabsIntent.launchUrl(context, authUri)
    }
}
