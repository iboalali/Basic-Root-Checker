package com.iboalali.basicrootchecker.analytics

import android.content.Context
import android.util.Log
import com.iboalali.basicrootchecker.BuildConfig
import com.iboalali.telemetry.TelemetryConfig
import com.iboalali.telemetry.TelemetryController
import com.telemetrydeck.sdk.TelemetryDeck

const val ERROR_CATEGORY_THROWN_EXCEPTION = "thrown-exception"
const val ERROR_CATEGORY_USER_INPUT = "user-input"
const val ERROR_CATEGORY_APP_STATE = "app-state"

// The "Other apps" telemetry vocabularies are owned by the shared catalog module
// (com.iboalali.appcatalog:data) and emitted verbatim. Both are cross-repo contracts, pinned there
// by OtherAppActionTest and CatalogRefreshResultTest:
//
//   otherAppClicked "action"    -> OtherAppAction:       "play_store", "launch", "website"
//   appCatalogRefresh "result"  -> CatalogRefreshResult: "updated", "not_modified", "failure"

object Analytics {

    /**
     * The whole TelemetryDeck lifecycle — the startup buffer, when the SDK starts, and the
     * start-before-flush ordering between them — lives in `com.iboalali.telemetry:core`. What stays
     * here is this app's own signal vocabulary, below.
     *
     * The two things it needs from this app are the ones a library cannot derive: the `BuildConfig`
     * app ID, and a logger (this app uses `android.util.Log`; the other two use Timber).
     */
    private val controller =
        TelemetryController(
            TelemetryConfig(
                appId = BuildConfig.TELEMETRY_DECK_APP_ID,
                debug = BuildConfig.DEBUG,
                onLookupFailure = { throwable, message -> Log.w(TAG, message, throwable) },
            )
        )

    private const val TAG = "Analytics"

    // Set once the device form factor has been reported, so config changes within a process
    // (rotation, fold/unfold, resize) don't emit the signal again — see [trackDeviceType].
    @Volatile private var deviceTypeReported = false

    /**
     * Resolve the telemetry opt-out preference read asynchronously at startup: start the SDK if
     * enabled and release the buffered signals, or discard them and stay silent.
     *
     * **Call on the main thread** — `TelemetryDeck.start` registers a process lifecycle observer.
     * The ordering that used to be this comment's job (start before flush, or the backlog is lost
     * into an uninitialized SDK) is now the controller's, and is covered by a test there.
     */
    fun resolveStartupPreference(context: Context, enabled: Boolean) =
        controller.resolve(context, enabled)

    /**
     * The Settings opt-out toggle.
     *
     * Takes a [Context] because opting in has to be able to *start* the SDK: a session that
     * launched opted-out never started it, and until this moved to the shared controller this app
     * only reopened its signal buffer here — so signals silently no-op'd for the rest of the
     * session and only resumed after a restart. Call on the main thread, as with
     * [resolveStartupPreference].
     */
    fun setEnabled(context: Context, enabled: Boolean) = controller.setEnabled(context, enabled)

    /**
     * Discard the persisted anonymous user identifier so future signals can't be linked to those
     * sent before — a fresh random identity is generated on the next signal. Performs file I/O, so
     * call off the main thread.
     */
    fun resetIdentity(context: Context) = controller.resetIdentity(context)

    /**
     * Run [action] now if telemetry is live, buffer it while the opt-out preference is still being
     * read at startup, or drop it once telemetry is known disabled.
     */
    private fun track(action: () -> Unit) = controller.submit(action)

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

    /**
     * The user acted on an entry in About → "Other apps". [action] is how they opened it:
     * `"play_store"`, `"launch"`, or `"website"` (owned by `OtherAppAction` in the shared catalog
     * module and emitted verbatim). [packageName] is the app's package, or its website/name for
     * entries without one (web apps).
     */
    fun trackOtherAppClicked(packageName: String, action: String) = track {
        TelemetryDeck.signal(
            "otherAppClicked",
            mapOf(
                "packageName" to packageName,
                "action" to action,
            ),
        )
    }

    /**
     * Outcome of the background "Other apps" catalog fetch run at launch. [result] is `"updated"`,
     * `"not_modified"`, or `"failure"` (owned by `CatalogRefreshResult` in the shared catalog
     * module and emitted verbatim); on failure [error] is the exception's simple name (e.g.
     * connectivity vs. parse), so offline launches can be told from real errors.
     */
    fun trackAppCatalogRefresh(result: String, error: String? = null) = track {
        val params = buildMap {
            put("result", result)
            if (!error.isNullOrEmpty()) put("error", error)
        }
        TelemetryDeck.signal("appCatalogRefresh", params)
    }

    fun trackRootCheckResult(result: String) = track {
        TelemetryDeck.signal(
            "rootCheckCompleted",
            mapOf("result" to result),
        )
    }

    fun trackRootProvider(provider: String, manager: String?, version: String?) = track {
        TelemetryDeck.signal(
            "rootProviderDetected",
            mapOf(
                "provider" to provider,
                "manager" to (manager ?: provider),
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

    /**
     * The in-app review gate opened and the Play-managed flow was requested (it may not actually
     * show).
     */
    fun trackReviewRequested() = track { TelemetryDeck.signal("reviewRequested") }

    /** The in-app review flow could not be launched (request/launch error from Play). */
    fun trackReviewFlowFailed(error: String) = track {
        TelemetryDeck.signal(
            "reviewFlowFailed",
            mapOf("error" to error),
        )
    }

    /** The explicit "Rate this app" link on the About screen was tapped. */
    fun trackRateLinkClicked() = track { TelemetryDeck.signal("rateLinkClicked") }

    /** [source] is where the tip jar was opened from — "settings" or "support_card". */
    fun trackTipJarOpened(source: String) = track {
        TelemetryDeck.signal(
            "tipJarOpened",
            mapOf("source" to source),
        )
    }

    /**
     * The main screen's support card actually appeared (the gate opened *and* nothing outranked
     * it). Reported from the UI rather than the gate so it can't claim a card the screen never drew
     * — pair it with [trackTipJarOpened] to read the card's conversion.
     *
     * One signal per process, so the count is distinct offers rather than appearances: a rotation,
     * or an update card handing the slot back, re-runs the reporting effect. `MainViewModel` holds
     * that guard, since the state it protects outlives the composition.
     */
    fun trackSupportCardShown() = track { TelemetryDeck.signal("supportCardShown") }

    /**
     * The support card was dismissed. [dismissCount] is the running total, capped by `SupportGate`.
     */
    fun trackSupportCardDismissed(dismissCount: Int) = track {
        TelemetryDeck.signal(
            "supportCardDismissed",
            mapOf("dismissCount" to dismissCount.toString()),
        )
    }

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
        hasVibrator: Boolean,
        primitiveClick: Boolean,
        envelopeEffectsSupported: Boolean,
        amplitudeControl: Boolean,
        sdkInt: Int,
    ) = track {
        TelemetryDeck.signal(
            "hapticCapabilities",
            mapOf(
                "hasVibrator" to hasVibrator.toString(),
                // Whether UI taps get the crisp click primitive (else a raw amplitude one-shot).
                "primitiveClick" to primitiveClick.toString(),
                "envelopeEffectsSupported" to envelopeEffectsSupported.toString(),
                "amplitudeControl" to amplitudeControl.toString(),
                "sdkInt" to sdkInt.toString(),
            ),
        )
    }

    /**
     * One-shot per cold start: the device form factor, so the tablet / large-screen audience can be
     * sized (e.g. to decide whether the Baseline Profile should also cover the expanded-width
     * dialog path). Idempotent within a process — only the first call emits, so a rotation,
     * fold/unfold, or resize after launch can't inflate the count.
     *
     * - [formFactor]: "phone" or "tablet", from the device's stable smallest width (≥600dp =
     *   tablet).
     * - [widthSizeClass]: the launch-time window width class — "compact", "medium", or "expanded".
     *   "expanded" (≥840dp) is exactly when the secondary screens open as a dialog; a foldable
     *   registers by its posture at launch (folded ≈ compact, unfolded ≈ expanded).
     */
    fun trackDeviceType(formFactor: String, widthSizeClass: String) {
        if (deviceTypeReported) return
        deviceTypeReported = true
        track {
            TelemetryDeck.signal(
                "deviceType",
                mapOf(
                    "formFactor" to formFactor,
                    "widthSizeClass" to widthSizeClass,
                ),
            )
        }
    }
}
