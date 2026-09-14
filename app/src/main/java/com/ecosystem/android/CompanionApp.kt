package com.ecosystem.android

import android.app.Application
import android.content.pm.ApplicationInfo
import com.ecosystem.core.common.log.AppLog
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CompanionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Logging exists only in debuggable builds.
        AppLog.isEnabled = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }
}
