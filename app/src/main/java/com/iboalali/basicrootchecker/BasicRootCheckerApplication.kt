package com.iboalali.basicrootchecker

import android.app.Application
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.data.UserPreferences
import com.telemetrydeck.sdk.TelemetryDeck

class BasicRootCheckerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val enabled = UserPreferences(this).telemetryEnabledBlocking()
        Analytics.setEnabled(enabled)

        if (enabled) {
            val builder = TelemetryDeck.Builder()
                .appID(BuildConfig.TELEMETRY_DECK_APP_ID)
                .showDebugLogs(BuildConfig.DEBUG)
            TelemetryDeck.start(applicationContext, builder)
        }
    }
}
