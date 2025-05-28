package app.secondclass.healthsyncer.worker

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import app.secondclass.healthsyncer.util.StravaPrefs
import java.util.Calendar

class Reset15MinReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    override fun onReceive(context: Context, intent: Intent?) {
        // Reset the counters
        val prefs = StravaPrefs.securePrefs(context)
        prefs.edit().apply {
            putInt(StravaPrefs.KEY_USER_REQUESTS_15M, 0)
            putInt(StravaPrefs.KEY_USER_READS_15M, 0)
            apply()
        }
        // Schedule the next alarm
        scheduleNext15MinReset(context)
    }
}

@RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
fun scheduleNext15MinReset(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, Reset15MinReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Calculate the next wall-clock 15-min boundary
    val cal = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        val minute = get(Calendar.MINUTE)
        val nextQuarter = ((minute / 15) + 1) * 15
        set(Calendar.MINUTE, nextQuarter % 60)
        if (nextQuarter >= 60) add(Calendar.HOUR_OF_DAY, 1)
    }
    val triggerAtMillis = cal.timeInMillis

    // Use the most exact method allowed
    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerAtMillis,
        pendingIntent
    )
}