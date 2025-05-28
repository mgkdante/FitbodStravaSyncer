package app.secondclass.healthsyncer

import android.app.Application
import app.secondclass.healthsyncer.worker.scheduleDailyReset

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        scheduleDailyReset(this)
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
