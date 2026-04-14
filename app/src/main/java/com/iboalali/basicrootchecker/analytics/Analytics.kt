package com.iboalali.basicrootchecker.analytics

import com.telemetrydeck.sdk.TelemetryDeck

object Analytics {

    fun trackNavigation(sourcePath: String, destinationPath: String) {
        TelemetryDeck.navigate(sourcePath, destinationPath)
    }

    fun trackRootCheckStarted() {
        TelemetryDeck.signal("rootCheckStarted")
    }

    fun trackRootCheckResult(result: String) {
        TelemetryDeck.signal(
            "rootCheckCompleted",
            mapOf("result" to result),
        )
    }
}
