package app.secondclass.healthsyncer.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.util.TimeZone
import javax.crypto.AEADBadTagException

/** single source of truth for the Strava token keys */
object StravaPrefs {
    const val PREFS_NAME   = "strava_prefs"
    const val KEY_ACCESS   = "access_token"
    const val KEY_REFRESH  = "refresh_token"
    const val KEY_EXPIRES  = "expires_at"
    const val KEY_REQUEST_COUNT_DAY = "api_requests_day"
    const val KEY_REQUEST_COUNT_15M = "api_requests_15min"
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

    const val CB_UPLOAD_LOCKED = "circuit_breaker_upload_locked"
    const val CB_UPLOAD_FAILURES = "circuit_breaker_upload_failures"
    const val CB_UPLOAD_LASTFAIL = "circuit_breaker_upload_lastfail"

    private const val LAST_ERROR_NOTIFICATION = "last_error_notification"


    fun shouldShowErrorNotification(context: Context, minIntervalMillis: Long = 10 * 60 * 1000): Boolean {
        val prefs = securePrefs(context)
        val last = prefs.getLong(LAST_ERROR_NOTIFICATION, 0)
        val now = System.currentTimeMillis()
        return (now - last) > minIntervalMillis
    }

    fun markErrorNotificationShown(context: Context) {
        securePrefs(context).edit().putLong(LAST_ERROR_NOTIFICATION, System.currentTimeMillis()).apply()
    }

    fun setUploadCircuitBreaker(context: Context, locked: Boolean) =
        securePrefs(context).edit { putBoolean(CB_UPLOAD_LOCKED, locked) }

    fun isUploadCircuitBreakerTripped(context: Context) =
        securePrefs(context).getBoolean(CB_UPLOAD_LOCKED, false)

    fun getUploadFailureCount(context: Context) =
        securePrefs(context).getInt(CB_UPLOAD_FAILURES, 0)

    fun incrementUploadFailureCount(context: Context) =
        securePrefs(context).edit {
            putInt(CB_UPLOAD_FAILURES, getUploadFailureCount(context) + 1)
            putLong(CB_UPLOAD_LASTFAIL, System.currentTimeMillis())
        }

    fun resetUploadFailureCount(context: Context) =
        securePrefs(context).edit {
            putInt(CB_UPLOAD_FAILURES, 0)
            putBoolean(CB_UPLOAD_LOCKED, false)
        }

    fun setLastFetchEpoch(context: Context, epoch: Long) =
        securePrefs(context).edit { putLong(KEY_LAST_FETCH_EPOCH, epoch) }

    fun incrementUserApiRequest(context: Context, isRead: Boolean) {
        val prefs = securePrefs(context)
        val now = System.currentTimeMillis()

        // --- 1. Daily window (still works as before)
        val dayStart = now / (24 * 60 * 60 * 1000)

        val prevDay = prefs.getLong(KEY_USER_REQUESTS_DAY_TS, -1)
        val dayReqCount = if (prevDay == dayStart) prefs.getInt(KEY_USER_REQUESTS_DAY, 0) else 0
        val dayReadCount = if (prevDay == dayStart) prefs.getInt(KEY_USER_READS_DAY, 0) else 0

        // --- 2. Strava-style 15-min UTC window ---
        val utcNow = System.currentTimeMillis() + TimeZone.getDefault().rawOffset // ensure UTC
        val mins = utcNow / 60000
        val quarter = mins / 15
        // The key: which 15-min block in UTC we are in
        val quarterHourWindow = quarter

        val prevQuarter = prefs.getLong(KEY_USER_REQUESTS_15M_TS, -1)
        val min15ReqCount = if (prevQuarter == quarterHourWindow) prefs.getInt(KEY_USER_REQUESTS_15M, 0) else 0
        val min15ReadCount = if (prevQuarter == quarterHourWindow) prefs.getInt(KEY_USER_READS_15M, 0) else 0

        prefs.edit {
            // Save window markers (so they roll over at right time)
            putLong(KEY_USER_REQUESTS_DAY_TS, dayStart)
            putLong(KEY_USER_REQUESTS_15M_TS, quarterHourWindow)

            // Increment counts, rolling over as needed
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
