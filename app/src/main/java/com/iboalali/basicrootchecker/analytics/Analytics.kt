package com.iboalali.basicrootchecker.analytics

import com.telemetrydeck.sdk.TelemetryDeck

const val ERROR_CATEGORY_THROWN_EXCEPTION = "thrown-exception"
const val ERROR_CATEGORY_USER_INPUT = "user-input"
const val ERROR_CATEGORY_APP_STATE = "app-state"

object Analytics {

    private val gate = SignalGate()

    /**
     * Resolve the telemetry opt-out preference, read asynchronously at startup.
     *
     * - enabled: switch to live and flush, in order, any signals buffered while
     *   the preference was still being read. MUST be called only after
     *   [TelemetryDeck.start] has run so the flushed signals reach an initialized SDK.
     * - disabled: drop everything buffered and stay silent.
     *
     * Also used by the Settings toggle at runtime, where the queue is already empty.
     */
    fun setEnabled(enabled: Boolean) = gate.resolve(enabled)

    /**
     * Run [action] now if telemetry is live, buffer it while the opt-out preference
     * is still being read at startup, or drop it once telemetry is known disabled.
     */
    private fun track(action: () -> Unit) = gate.submit(action)

    fun trackError(
        id: String,
        message: String? = null,
        category: String = ERROR_CATEGORY_THROWN_EXCEPTION,
    ) = track {
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

    fun trackNavigation(sourcePath: String, destinationPath: String) = track {
        TelemetryDeck.navigate(sourcePath, destinationPath)
    }

    fun trackRootCheckStarted() = track { TelemetryDeck.signal("rootCheckStarted") }

    fun trackRootRequested() = track { TelemetryDeck.signal("rootRequested") }

    fun trackPrivacyPolicyClicked() = track { TelemetryDeck.signal("privacyPolicyClicked") }

    fun trackLanguageChanged(tag: String) = track {
        TelemetryDeck.signal(
            "languageChanged",
            mapOf("language" to tag),
        )
    }

    fun trackSocialLinkClicked(platform: String) = track {
        TelemetryDeck.signal(
            "socialLinkClicked",
            mapOf("platform" to platform),
        )
    }

    fun trackOtherAppClicked(packageName: String) = track {
        TelemetryDeck.signal(
            "otherAppClicked",
            mapOf("packageName" to packageName),
        )
    }

    fun trackRootCheckResult(result: String) = track {
        TelemetryDeck.signal(
            "rootCheckCompleted",
            mapOf("result" to result),
        )
    }

    fun trackRootProvider(provider: String, version: String?) = track {
        TelemetryDeck.signal(
            "rootProviderDetected",
            mapOf(
                "provider" to provider,
                "version" to (version ?: ""),
            ),
        )
    }

    fun trackUpdateAvailable() = track { TelemetryDeck.signal("updateAvailable") }

    fun trackUpdateStarted() = track { TelemetryDeck.signal("updateStarted") }

    fun trackUpdateDownloaded() = track { TelemetryDeck.signal("updateDownloaded") }

    fun trackUpdateFailed(error: String) = track {
        TelemetryDeck.signal(
            "updateFailed",
            mapOf("error" to error),
        )
    }

    fun trackTipJarOpened() = track { TelemetryDeck.signal("tipJarOpened") }

    fun trackTipSelected(tier: String) = track {
        TelemetryDeck.signal(
            "tipSelected",
            mapOf("tier" to tier),
        )
    }

    /** [variant] is "record" (first, durable) or "repeat" (consumable). */
    fun trackTipPurchased(productId: String, tier: String, variant: String) = track {
        TelemetryDeck.signal(
            "tipPurchased",
            mapOf(
                "productId" to productId,
                "tier" to tier,
                "variant" to variant,
            ),
        )
    }

    /** A deferred-payment purchase awaiting completion. */
    fun trackTipPending(productId: String, tier: String) = track {
        TelemetryDeck.signal(
            "tipPending",
            mapOf("productId" to productId, "tier" to tier),
        )
    }

    fun trackTipCanceled(tier: String) = track {
        TelemetryDeck.signal(
            "tipCanceled",
            mapOf("tier" to tier),
        )
    }

    fun trackTipFailed(reason: String) = track {
        TelemetryDeck.signal(
            "tipFailed",
            mapOf("reason" to reason),
        )
    }

    /** Play Billing could not connect (e.g. no Play services). [code] is the response code. */
    fun trackBillingUnavailable(code: String) = track {
        TelemetryDeck.signal(
            "billingUnavailable",
            mapOf("code" to code),
        )
    }

    /** Tip products failed to load (query error, or none configured). */
    fun trackTipProductsUnavailable(reason: String) = track {
        TelemetryDeck.signal(
            "tipProductsUnavailable",
            mapOf("reason" to reason),
        )
    }

    fun trackHapticCapabilities(
        envelopeEffectsSupported: Boolean,
        amplitudeControl: Boolean,
        sdkInt: Int,
    ) = track {
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
