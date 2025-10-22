package com.ssj.statuswindow

import android.app.Application
import com.ssj.statuswindow.BuildConfig
import timber.log.Timber

class StatusWindowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("StatusWindowApp started")
        }
    }
}
