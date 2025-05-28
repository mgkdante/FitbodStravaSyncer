package app.secondclass.healthsyncer

import android.app.Application
import app.secondclass.healthsyncer.worker.scheduleDailyReset
import app.secondclass.healthsyncer.worker.scheduleNext15MinReset

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        scheduleDailyReset(this)
        scheduleNext15MinReset(this) // <-- Add this!
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
