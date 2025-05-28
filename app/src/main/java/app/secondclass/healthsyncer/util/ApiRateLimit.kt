package app.secondclass.healthsyncer.util

import android.content.Context

object ApiRateLimitUtil {
    fun getApiResetTimeHint(context: Context): String {
        val now = System.currentTimeMillis()
        val next15ResetMillis = StravaPrefs.getApiLimitReset(context)
        if (next15ResetMillis > now) {
            val min = (next15ResetMillis - now) / 60000
            val sec = ((next15ResetMillis - now) / 1000) % 60
            return "${min}m ${sec}s"
        }
        val min15Start = now / (15 * 60 * 1000)
        val next15Reset = ((min15Start + 1) * 15 * 60 * 1000) - now
        val min = next15Reset / 60000
        val sec = (next15Reset / 1000) % 60
        return "${min}m ${sec}s"
    }
}
