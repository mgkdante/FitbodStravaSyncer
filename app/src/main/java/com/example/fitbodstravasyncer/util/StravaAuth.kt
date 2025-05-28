package com.example.fitbodstravasyncer.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.AEADBadTagException

/** single source of truth for the Strava token keys */
object StravaPrefs {
    const val PREFS_NAME   = "strava_prefs"
    const val KEY_ACCESS   = "access_token"
    const val KEY_REFRESH  = "refresh_token"
    const val KEY_EXPIRES  = "expires_at"
    const val KEY_REQUEST_COUNT_DAY = "api_requests_day"
    const val KEY_REQUEST_COUNT_15M = "api_requests_15min"
    const val KEY_REQUEST_TIMESTAMP_DAY = "api_requests_day_ts"
    const val KEY_REQUEST_TIMESTAMP_15M = "api_requests_15min_ts"
    private const val MASTER_KEY_ALIAS = MasterKey.DEFAULT_MASTER_KEY_ALIAS
    const val KEY_LAST_FETCH_EPOCH = "last_strava_fetch_epoch"

    fun getLastFetchEpoch(context: Context): Long =
        securePrefs(context).getLong(KEY_LAST_FETCH_EPOCH, 0L)

    fun setLastFetchEpoch(context: Context, epoch: Long) =
        securePrefs(context).edit { putLong(KEY_LAST_FETCH_EPOCH, epoch) }

    fun incrementApiRequestCount(context: Context) {
        val prefs = securePrefs(context)
        val now = System.currentTimeMillis()
        val dayStart = now / (24 * 60 * 60 * 1000)
        val min15Start = now / (15 * 60 * 1000)
        val prevDay = prefs.getLong(KEY_REQUEST_TIMESTAMP_DAY, -1)
        val prev15m = prefs.getLong(KEY_REQUEST_TIMESTAMP_15M, -1)
        val dayCount = if (prevDay == dayStart) prefs.getInt(KEY_REQUEST_COUNT_DAY, 0) else 0
        val min15Count = if (prev15m == min15Start) prefs.getInt(KEY_REQUEST_COUNT_15M, 0) else 0
        prefs.edit {
            putLong(KEY_REQUEST_TIMESTAMP_DAY, dayStart)
            putLong(KEY_REQUEST_TIMESTAMP_15M, min15Start)
            putInt(KEY_REQUEST_COUNT_DAY, dayCount + 1)
            putInt(KEY_REQUEST_COUNT_15M, min15Count + 1)
        }
    }

    fun isApiLimitNear(context: Context): Boolean {
        val day = getApiRequestCountDay(context)
        val min15 = getApiRequestCount15Min(context)
        // You can tweak the warning threshold (e.g., 90% of the limit)
        return day >= 1800 || min15 >= 180
    }


    fun getApiRequestCountDay(context: Context): Int =
        securePrefs(context).getInt(KEY_REQUEST_COUNT_DAY, 0)

    fun getApiRequestCount15Min(context: Context): Int =
        securePrefs(context).getInt(KEY_REQUEST_COUNT_15M, 0)

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
