package com.example.fitbodstravasyncer

import android.app.Application
import com.example.fitbodstravasyncer.worker.scheduleDailyReset

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
