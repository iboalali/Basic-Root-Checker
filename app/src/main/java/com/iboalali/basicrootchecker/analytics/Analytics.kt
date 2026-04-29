package com.iboalali.basicrootchecker.analytics

import com.telemetrydeck.sdk.TelemetryDeck

object Analytics {

    @Volatile
    private var enabled: Boolean = false

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun trackNavigation(sourcePath: String, destinationPath: String) {
        if (!enabled) return
        TelemetryDeck.navigate(sourcePath, destinationPath)
    }

    fun trackRootCheckStarted() {
        if (!enabled) return
        TelemetryDeck.signal("rootCheckStarted")
    }

    fun trackPrivacyPolicyClicked() {
        if (!enabled) return
        TelemetryDeck.signal("privacyPolicyClicked")
    }

    fun trackWebsiteClicked() {
        if (!enabled) return
        TelemetryDeck.signal("websiteClicked")
    }

    fun trackOtherAppClicked(packageName: String) {
        if (!enabled) return
        TelemetryDeck.signal(
            "otherAppClicked",
            mapOf("packageName" to packageName),
        )
    }

    fun trackRootCheckResult(result: String) {
        if (!enabled) return
        TelemetryDeck.signal(
            "rootCheckCompleted",
            mapOf("result" to result),
        )
    }

    fun trackUpdateAvailable() {
        if (!enabled) return
        TelemetryDeck.signal("updateAvailable")
    }

    fun trackUpdateStarted() {
        if (!enabled) return
        TelemetryDeck.signal("updateStarted")
    }

    fun trackUpdateDownloaded() {
        if (!enabled) return
        TelemetryDeck.signal("updateDownloaded")
    }

    fun trackUpdateFailed(error: String) {
        if (!enabled) return
        TelemetryDeck.signal(
            "updateFailed",
            mapOf("error" to error),
        )
    }
}
