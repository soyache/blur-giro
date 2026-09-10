package com.soyache.blurgiro

import android.app.Application
import com.soyache.blurgiro.data.AppSettings

class CristalGiroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppSettings.get(this)
    }
}
