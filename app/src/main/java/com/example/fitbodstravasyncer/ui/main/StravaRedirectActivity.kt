package com.example.fitbodstravasyncer.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.fitbodstravasyncer.data.strava.StravaAuthService
import com.example.fitbodstravasyncer.data.strava.StravaConstants.CLIENT_ID
import com.example.fitbodstravasyncer.data.strava.StravaConstants.CLIENT_SECRET
import com.example.fitbodstravasyncer.util.StravaPrefs
import kotlinx.coroutines.launch
import retrofit2.HttpException

class StravaRedirectActivity : ComponentActivity() {

    private companion object {
        private const val SCHEME = "myapp"
        private const val HOST   = "strava-auth"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val code = intent.data
            ?.takeIf { it.scheme == SCHEME && it.host == HOST }
            ?.getQueryParameter("code")

        if (code == null) { finish(); return }

        val prefs = StravaPrefs.securePrefs(this)
        lifecycleScope.launch {
            try {
                val resp = StravaAuthService.Companion.create()
                    .exchangeCode(CLIENT_ID, CLIENT_SECRET, code)

                if (resp.accessToken.isNullOrBlank() || resp.refreshToken.isNullOrBlank() || resp.expiresAt == null) {
                    // Optionally show error UI here if you want
                    return@launch
                }

                prefs.edit().apply {
                    putString(StravaPrefs.KEY_ACCESS, resp.accessToken)
                    putString(StravaPrefs.KEY_REFRESH, resp.refreshToken)
                    putLong  (StravaPrefs.KEY_EXPIRES, resp.expiresAt)
                    apply()
                }

                // <-- THIS IS THE KEY: Pass extra for snackbar!
                startActivity(
                    Intent(this@StravaRedirectActivity, MainActivity::class.java)
                        .addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                        )
                        .putExtra("show_strava_connected_snackbar", true)
                )
            } catch (e: Exception) {
                // Optionally show error UI here
                if (e is HttpException) {
                    // Handle API error if needed
                }
            } finally {
                finish()
            }
        }
    }
}
