package com.iboalali.basicrootchecker.analytics

import com.telemetrydeck.sdk.TelemetryDeck

object Analytics {

    fun trackNavigation(sourcePath: String, destinationPath: String) {
        TelemetryDeck.navigate(sourcePath, destinationPath)
    }

    fun trackRootCheckStarted() {
        TelemetryDeck.signal("rootCheckStarted")
    }

    fun trackPrivacyPolicyClicked() {
        TelemetryDeck.signal("privacyPolicyClicked")
    }

    fun trackOtherAppClicked(packageName: String) {
        TelemetryDeck.signal(
            "otherAppClicked",
            mapOf("packageName" to packageName),
        )
    }

    fun trackRootCheckResult(result: String) {
        TelemetryDeck.signal(
            "rootCheckCompleted",
            mapOf("result" to result),
        )
    }
}
