package com.example.fitbodstravasyncer.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** single source of truth for the Strava token keys */
object StravaPrefs {
    const val PREFS_NAME   = "strava_prefs"
    const val KEY_ACCESS   = "access_token"
    const val KEY_REFRESH  = "refresh_token"
    const val KEY_EXPIRES  = "expires_at"
    const val KEY_LAST_SYNCED_ACTIVITY_TIME = "last_synced_activity_time"
    const val KEY_DAILY_SYNC = "daily_sync_enabled"



    fun securePrefs(context: Context): SharedPreferences =
        EncryptedSharedPreferences.create(
            context, PREFS_NAME,
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
}

/** Are we really connected?  ->  both tokens must exist */
fun Context.isStravaConnected(): Boolean =
    StravaPrefs.securePrefs(this).let { p ->
        !p.getString(StravaPrefs.KEY_ACCESS,  null).isNullOrBlank() &&
                !p.getString(StravaPrefs.KEY_REFRESH, null).isNullOrBlank()
    }
