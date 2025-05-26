package com.example.fitbodstravasyncer.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.AEADBadTagException

/** single source of truth for the Strava token keys */
object StravaPrefs {
    const val PREFS_NAME   = "strava_prefs"
    const val KEY_ACCESS   = "access_token"
    const val KEY_REFRESH  = "refresh_token"
    const val KEY_EXPIRES  = "expires_at"
    private const val MASTER_KEY_ALIAS = MasterKey.DEFAULT_MASTER_KEY_ALIAS


    fun securePrefs(context: Context): SharedPreferences {
        return try {
            // 1) Normal creation with existing master key
            val masterKey = MasterKey.Builder(context, MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: AEADBadTagException) {
            // 2) Corruption detected: remove Keystore alias
            KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
                deleteEntry(MASTER_KEY_ALIAS)
            }
            // 3) Remove all prefs files (XML + WAL + SHM)
            context.deleteSharedPreferences(PREFS_NAME)

            // 4) Recreate a fresh master key
            val newMasterKey = MasterKey.Builder(context, MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            // 5) Recreate EncryptedSharedPreferences
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                newMasterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}

/** Returns true if both access & refresh tokens exist (i.e. Strava is connected) */
fun Context.isStravaConnected(): Boolean {
    val prefs = StravaPrefs.securePrefs(this)
    return !prefs.getString(StravaPrefs.KEY_ACCESS, null).isNullOrBlank() &&
            !prefs.getString(StravaPrefs.KEY_REFRESH, null).isNullOrBlank()
}