package com.example.fitbodstravasyncer.util

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.example.fitbodstravasyncer.data.strava.StravaAuthService
import com.example.fitbodstravasyncer.data.strava.StravaConstants.CLIENT_ID
import com.example.fitbodstravasyncer.data.strava.StravaConstants.CLIENT_SECRET
import java.util.concurrent.CancellationException

object StravaTokenManager {
    suspend fun getValidAccessToken(ctx: Context): String {
        val prefs   = StravaPrefs.securePrefs(ctx)
        var access  = prefs.getString(StravaPrefs.KEY_ACCESS, null)
        val refresh = prefs.getString(StravaPrefs.KEY_REFRESH, null)
        val expires = prefs.getLong(StravaPrefs.KEY_EXPIRES, 0L)
        val now     = System.currentTimeMillis() / 1000

        if (access == null && refresh == null) {
            throw CancellationException("STRAVA_NOT_CONNECTED")
        }

        if (access == null || now >= expires - 60) {
            check(!refresh.isNullOrBlank()) { "Missing refresh token" }

            val resp = StravaAuthService.Companion.create()
                .refreshToken(CLIENT_ID, CLIENT_SECRET, refresh)

            access = resp.accessToken
            if (resp.accessToken.isNullOrBlank() || resp.refreshToken.isNullOrBlank() || resp.expiresAt == null) {
                Log.w("StravaAuth", "Invalid token response: $resp")
                // Optionally handle the error
            } else {
                prefs.edit(commit = true) {
                    putString(StravaPrefs.KEY_ACCESS, resp.accessToken)
                    putString(StravaPrefs.KEY_REFRESH, resp.refreshToken)
                    putLong(StravaPrefs.KEY_EXPIRES, resp.expiresAt)
                } // <-- actually persists it
            }
        }

        return access!!
    }
}