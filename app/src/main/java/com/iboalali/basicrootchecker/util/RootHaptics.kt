package com.iboalali.basicrootchecker.util

import android.content.Context
import android.util.Log
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.haptics.HapticWaveform
import com.iboalali.haptics.Haptics

/**
 * This app's vibration vocabulary: the checking ramp and the three outcome buzzes that accompany a
 * root check.
 *
 * The engine underneath — actuator selection, the SDK tiers, the OEM workarounds — is
 * `com.iboalali.haptics:core`, shared with Billboard and HPN and extracted from *this* file. What
 * stays here is what is genuinely Basic Root Checker's: which effect means what, and reporting the
 * device's actuator capabilities as a signal.
 *
 * Per `haptics-conventions`, these richer effects are played from the ViewModel, not from the UI —
 * a separate concern from the tap feedback that `:haptics:compose` wraps, even though both run
 * through [haptics] and both honour the same in-app setting.
 */
class RootHaptics(context: Context) {

    /**
     * The shared engine, also handed to `LocalAppHaptics` so the Compose tap wrappers and these
     * outcome effects drive one instance — which is what keeps a tap from cutting off a result
     * buzz.
     */
    val haptics: Haptics = Haptics(context) { throwable, id -> Log.e(TAG, id, throwable) }

    init {
        val c = haptics.capabilities
        Log.i(
            TAG,
            "capabilities: sdk=${c.sdk} hasVibrator=${c.hasVibrator} " +
                "primitiveClick=${c.primitiveClick} envelopeEffectsSupported=${c.envelopeEffects} " +
                "amplitudeControl=${c.amplitudeControl} freqProfile=${c.frequencyProfile ?: "n/a"}",
        )
        Analytics.trackHapticCapabilities(
            c.hasVibrator,
            c.primitiveClick,
            c.envelopeEffects,
            c.amplitudeControl,
            c.sdk,
        )
    }

    /**
     * Starts the rising-frequency sweep that plays while a check runs, until [cancel].
     *
     * Only actuators supporting wave-envelope effects can render it; everywhere else this is a
     * no-op and the user feels only the outcome buzz. That is deliberate — a substitute buzz here
     * would read as a result arriving early.
     */
    fun startCheckingRamp() {
        if (!haptics.playFrequencySweep()) {
            Log.i(TAG, "startCheckingRamp: no usable wave-envelope support — result buzz only")
        }
    }

    /** A short pulse then a longer one ("dot-daaat") for a rooted result. */
    fun playSuccess(): Unit = haptics.playWaveform(SUCCESS)

    /** Two equal short pulses ("dot-dot") for a not-rooted / unknown result. */
    fun playError(): Unit = haptics.playWaveform(ERROR)

    /** A single soft pulse for the "root installed but not granted" result. */
    fun playNeutral(): Unit = haptics.playWaveform(NEUTRAL)

    /** Stops any ongoing vibration. Safe to call when nothing is playing. */
    fun cancel(): Unit = haptics.cancel()

    companion object {
        private const val TAG = "RootHaptics"

        // Each effect carries both representations because the device decides which is playable:
        // the
        // amplitude pair where the actuator has amplitude control, the duration pattern otherwise.
        // The duration pattern's leading 0 is the platform's initial *off* delay, not a mistake.

        /** Success "dot-daaat": a short medium pulse, a gap, then a longer full-strength pulse. */
        private val SUCCESS =
            HapticWaveform(
                amplitudeTimings = longArrayOf(90, 50, 200),
                amplitudes = intArrayOf(150, 0, 255),
                durationPattern = longArrayOf(0, 40, 90, 200),
            )

        /** Error "dot-dot": two equal short pulses. */
        private val ERROR =
            HapticWaveform(
                amplitudeTimings = longArrayOf(60, 100, 60),
                amplitudes = intArrayOf(255, 0, 255),
                durationPattern = longArrayOf(0, 60, 100, 60),
            )

        /** Neutral: a single soft pulse. */
        private val NEUTRAL =
            HapticWaveform(
                amplitudeTimings = longArrayOf(90),
                amplitudes = intArrayOf(120),
                durationPattern = longArrayOf(0, 90),
            )
    }
}
