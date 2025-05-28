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
    const val KEY_API_LIMIT_RESET = "api_limit_reset"

    const val KEY_USER_REQUESTS_15M = "user_api_requests_15m"
    const val KEY_USER_REQUESTS_DAY = "user_api_requests_day"
    const val KEY_USER_READS_15M = "user_api_reads_15m"
    const val KEY_USER_READS_DAY = "user_api_reads_day"
    const val KEY_USER_REQUESTS_15M_TS = "user_api_requests_15m_ts"
    const val KEY_USER_REQUESTS_DAY_TS = "user_api_requests_day_ts"

    const val USER_READS_15M_LIMIT = 90
    const val USER_REQUESTS_15M_LIMIT = 180
    const val USER_READS_DAY_LIMIT = 900
    const val USER_REQUESTS_DAY_LIMIT = 1800


    fun getLastFetchEpoch(context: Context): Long =
        securePrefs(context).getLong(KEY_LAST_FETCH_EPOCH, 0L)

    fun setLastFetchEpoch(context: Context, epoch: Long) =
        securePrefs(context).edit { putLong(KEY_LAST_FETCH_EPOCH, epoch) }

    fun incrementUserApiRequest(context: Context, isRead: Boolean) {
        val prefs = securePrefs(context)
        val now = System.currentTimeMillis()
        val dayStart = now / (24 * 60 * 60 * 1000)
        val min15Start = now / (15 * 60 * 1000)

        val prevDay = prefs.getLong(KEY_USER_REQUESTS_DAY_TS, -1)
        val prev15m = prefs.getLong(KEY_USER_REQUESTS_15M_TS, -1)
        val dayReqCount = if (prevDay == dayStart) prefs.getInt(KEY_USER_REQUESTS_DAY, 0) else 0
        val min15ReqCount = if (prev15m == min15Start) prefs.getInt(KEY_USER_REQUESTS_15M, 0) else 0
        val dayReadCount = if (prevDay == dayStart) prefs.getInt(KEY_USER_READS_DAY, 0) else 0
        val min15ReadCount = if (prev15m == min15Start) prefs.getInt(KEY_USER_READS_15M, 0) else 0

        prefs.edit {
            putLong(KEY_USER_REQUESTS_DAY_TS, dayStart)
            putLong(KEY_USER_REQUESTS_15M_TS, min15Start)
            putInt(KEY_USER_REQUESTS_DAY, dayReqCount + 1)
            putInt(KEY_USER_REQUESTS_15M, min15ReqCount + 1)
            if (isRead) {
                putInt(KEY_USER_READS_DAY, dayReadCount + 1)
                putInt(KEY_USER_READS_15M, min15ReadCount + 1)
            }
        }
    }

    fun getUserApiRequestCount15Min(context: Context): Int =
        securePrefs(context).getInt(KEY_USER_REQUESTS_15M, 0)
    fun getUserApiRequestCountDay(context: Context): Int =
        securePrefs(context).getInt(KEY_USER_REQUESTS_DAY, 0)
    fun getUserApiReadCount15Min(context: Context): Int =
        securePrefs(context).getInt(KEY_USER_READS_15M, 0)
    fun getUserApiReadCountDay(context: Context): Int =
        securePrefs(context).getInt(KEY_USER_READS_DAY, 0)


    fun isUserApiLimitNear(context: Context): Boolean {
        return getUserApiReadCount15Min(context) >= (USER_READS_15M_LIMIT * 0.9).toInt() ||
                getUserApiRequestCount15Min(context) >= (USER_REQUESTS_15M_LIMIT * 0.9).toInt() ||
                getUserApiReadCountDay(context) >= (USER_READS_DAY_LIMIT * 0.9).toInt() ||
                getUserApiRequestCountDay(context) >= (USER_REQUESTS_DAY_LIMIT * 0.9).toInt()
    }



    fun isUserApiLimitReached(context: Context): Boolean {
        return getUserApiReadCount15Min(context) >= USER_READS_15M_LIMIT ||
                getUserApiRequestCount15Min(context) >= USER_REQUESTS_15M_LIMIT ||
                getUserApiReadCountDay(context) >= USER_READS_DAY_LIMIT ||
                getUserApiRequestCountDay(context) >= USER_REQUESTS_DAY_LIMIT
    }


    fun disconnect(context: Context) {
        securePrefs(context).edit {
            remove(KEY_ACCESS)
            remove(KEY_REFRESH)
            remove(KEY_EXPIRES)
            remove(KEY_LAST_FETCH_EPOCH)
        }
    }

    fun setApiLimitReset(context: Context, resetTime: Long) = securePrefs(context).edit {
        putLong(KEY_API_LIMIT_RESET, resetTime)
    }
    fun getApiLimitReset(context: Context): Long =
        securePrefs(context).getLong(KEY_API_LIMIT_RESET, 0)

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
