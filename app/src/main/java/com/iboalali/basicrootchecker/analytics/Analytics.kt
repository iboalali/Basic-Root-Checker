package com.iboalali.basicrootchecker.analytics

import com.telemetrydeck.sdk.TelemetryDeck

const val ERROR_CATEGORY_THROWN_EXCEPTION = "thrown-exception"
const val ERROR_CATEGORY_USER_INPUT = "user-input"
const val ERROR_CATEGORY_APP_STATE = "app-state"

object Analytics {

    @Volatile
    private var enabled: Boolean = false

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun trackError(
        id: String,
        message: String? = null,
        category: String = ERROR_CATEGORY_THROWN_EXCEPTION,
    ) {
        if (!enabled) return
        val params = buildMap {
            put("TelemetryDeck.Error.id", id)
            if (!message.isNullOrEmpty()) put("TelemetryDeck.Error.message", message)
            put("TelemetryDeck.Error.category", category)
        }
        TelemetryDeck.signal("TelemetryDeck.Error.occurred", params)
    }

    fun trackError(
        throwable: Throwable,
        id: String? = null,
        category: String = ERROR_CATEGORY_THROWN_EXCEPTION,
    ) {
        val errorId = id ?: throwable::class.simpleName ?: "UnknownThrowable"
        trackError(errorId, throwable.message, category)
    }

    fun trackNavigation(sourcePath: String, destinationPath: String) {
        if (!enabled) return
        TelemetryDeck.navigate(sourcePath, destinationPath)
    }

    fun trackRootCheckStarted() {
        if (!enabled) return
        TelemetryDeck.signal("rootCheckStarted")
    }

    fun trackRootRequested() {
        if (!enabled) return
        TelemetryDeck.signal("rootRequested")
    }

    fun trackPrivacyPolicyClicked() {
        if (!enabled) return
        TelemetryDeck.signal("privacyPolicyClicked")
    }

    fun trackSocialLinkClicked(platform: String) {
        if (!enabled) return
        TelemetryDeck.signal(
            "socialLinkClicked",
            mapOf("platform" to platform),
        )
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

    fun trackRootProvider(provider: String, version: String?) {
        if (!enabled) return
        TelemetryDeck.signal(
            "rootProviderDetected",
            mapOf(
                "provider" to provider,
                "version" to (version ?: ""),
            ),
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

    fun trackHapticCapabilities(
        envelopeEffectsSupported: Boolean,
        amplitudeControl: Boolean,
        sdkInt: Int,
    ) {
        if (!enabled) return
        TelemetryDeck.signal(
            "hapticCapabilities",
            mapOf(
                "envelopeEffectsSupported" to envelopeEffectsSupported.toString(),
                "amplitudeControl" to amplitudeControl.toString(),
                "sdkInt" to sdkInt.toString(),
            ),
        )
    }
}
