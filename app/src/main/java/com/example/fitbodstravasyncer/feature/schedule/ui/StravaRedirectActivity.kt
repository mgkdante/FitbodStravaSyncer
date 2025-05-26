package com.example.fitbodstravasyncer.feature.schedule.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.fitbodstravasyncer.MainActivity
import com.example.fitbodstravasyncer.data.strava.StravaAuthService
import com.example.fitbodstravasyncer.util.StravaKeys
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
        Log.d("STRAVA-log", "Redirect Activity launched")

        val code = intent.data
            ?.takeIf { it.scheme == SCHEME && it.host == HOST }
            ?.getQueryParameter("code")

        Log.d("STRAVA-log", "Got code: $code")
        if (code == null) { finish(); return }

        val prefs = StravaPrefs.securePrefs(this)
        lifecycleScope.launch {
            try {
                Log.d(
                    "STRAVA-log",
                    "CLIENT_ID=${StravaKeys.CLIENT_ID}, CLIENT_SECRET=${StravaKeys.CLIENT_SECRET}"
                )
                val resp = StravaAuthService.Companion.create()
                    .exchangeCode(StravaKeys.CLIENT_ID, StravaKeys.CLIENT_SECRET, code)
                Log.d("STRAVA-log", "Raw token response: $resp")

                if (resp.accessToken.isNullOrBlank() || resp.refreshToken.isNullOrBlank() || resp.expiresAt == null) {
                    Log.w("STRAVA-log", "WARNING: HTTP 200 but token fields are null/blank! resp=$resp")
                    Toast.makeText(
                        this@StravaRedirectActivity,
                        "Strava auth failed. Check API credentials or response format.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
                else {
                    Log.d("STRAVA-log", "✅ Successfully received and saving tokens")
                }

                prefs.edit().apply {
                    putString(StravaPrefs.KEY_ACCESS, resp.accessToken)
                    putString(StravaPrefs.KEY_REFRESH, resp.refreshToken)
                    putLong  (StravaPrefs.KEY_EXPIRES, resp.expiresAt)  // ✅ force unwrap
                    apply()
                }

                val a = prefs.getString(StravaPrefs.KEY_ACCESS, null)
                val r = prefs.getString(StravaPrefs.KEY_REFRESH, null)
                Log.d("STRAVA-log", "Tokens after save: access=$a refresh=$r")

                startActivity(
                    Intent(this@StravaRedirectActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra("strava_connected", true)
                )
            }catch (e: Exception) {
                Log.e("STRAVA-log", "Auth failed", e)
                if (e is HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e("STRAVA-log", "HTTP error body: $errorBody")
                }
                Toast.makeText(
                    this@StravaRedirectActivity,
                    "Strava authentication failed",
                    Toast.LENGTH_LONG
                ).show()
        }
            finally { finish() }
        }
    }
}