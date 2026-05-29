package com.iboalali.basicrootchecker.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.analytics.ERROR_CATEGORY_APP_STATE

/**
 * Plays the vibration feedback that accompanies a root check: an accelerating, intensifying "ramp"
 * while the check runs, then a short outcome buzz once the result is known.
 *
 * The [Vibrator] is resolved once and the helper degrades gracefully across the supported API range
 * (minSdk 23): amplitude-controlled waveforms when the motor supports them (API 26+), an on/off
 * waveform when it does not, and the legacy [Vibrator.vibrate] pattern API on API 23-25.
 */
class RootHaptics(context: Context) {

    private val vibrator: Vibrator? = resolveVibrator(context)

    private val available: Boolean
        get() = vibrator?.hasVibrator() == true

    /** Accelerating, intensifying pulses that loop a strong fast buzz until [cancel] is called. */
    fun startCheckingRamp() = play(
        "haptic-ramp",
        RAMP_AMPLITUDE_TIMINGS, RAMP_AMPLITUDES, RAMP_AMPLITUDE_REPEAT,
        RAMP_PATTERN, RAMP_PATTERN_REPEAT,
    )

    /** A short pulse then a longer one ("dot-daaat") for a rooted result. */
    fun playSuccess() = play(
        "haptic-success",
        SUCCESS_AMPLITUDE_TIMINGS, SUCCESS_AMPLITUDES, NO_REPEAT,
        SUCCESS_PATTERN, NO_REPEAT,
    )

    /** Two equal short pulses ("dot-dot") for a not-rooted / unknown result. */
    fun playError() = play(
        "haptic-error",
        ERROR_AMPLITUDE_TIMINGS, ERROR_AMPLITUDES, NO_REPEAT,
        ERROR_PATTERN, NO_REPEAT,
    )

    /** A single soft pulse for the "root installed but not granted" result. */
    fun playNeutral() = play(
        "haptic-neutral",
        NEUTRAL_AMPLITUDE_TIMINGS, NEUTRAL_AMPLITUDES, NO_REPEAT,
        NEUTRAL_PATTERN, NO_REPEAT,
    )

    /** Stops any ongoing vibration. Safe to call when nothing is playing. */
    fun cancel() {
        if (!available) return
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "cancel: ", e)
            Analytics.trackError(e, id = "haptic-cancel", category = ERROR_CATEGORY_APP_STATE)
        }
    }

    /**
     * Cancels whatever is playing and starts the given effect. [amplitudeTimings]/[amplitudes] feed
     * the amplitude-controlled waveform; [durationPattern] is the on/off fallback used both by the
     * no-amplitude waveform and the legacy API, so its first entry is an initial off delay.
     */
    private fun play(
        id: String,
        amplitudeTimings: LongArray,
        amplitudes: IntArray,
        amplitudeRepeat: Int,
        durationPattern: LongArray,
        durationRepeat: Int,
    ) {
        val v = vibrator ?: return
        if (!available) return
        try {
            v.cancel()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (v.hasAmplitudeControl()) {
                    VibrationEffect.createWaveform(amplitudeTimings, amplitudes, amplitudeRepeat)
                } else {
                    VibrationEffect.createWaveform(durationPattern, durationRepeat)
                }
                v.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationPattern, durationRepeat)
            }
        } catch (e: Exception) {
            Log.e(TAG, "play $id: ", e)
            Analytics.trackError(e, id = id, category = ERROR_CATEGORY_APP_STATE)
        }
    }

    private fun resolveVibrator(context: Context): Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Exception) {
        Log.e(TAG, "resolveVibrator: ", e)
        Analytics.trackError(e, id = "haptic-resolve", category = ERROR_CATEGORY_APP_STATE)
        null
    }

    companion object {
        private const val TAG = "RootHaptics"
        private const val NO_REPEAT = -1

        // Ramp, amplitude path: paired [duration, amplitude]; on segments grow and intensify while
        // off gaps shrink. Loops the final strong segment (index 16) so it sustains during a slow
        // first-grant request and is simply cut short for the normal ~1s check.
        private val RAMP_AMPLITUDE_TIMINGS = longArrayOf(
            20, 140, 25, 110, 30, 85, 35, 65, 40, 45, 50, 30, 60, 18, 70, 12, 90, 10,
        )
        private val RAMP_AMPLITUDES = intArrayOf(
            70, 0, 100, 0, 130, 0, 160, 0, 190, 0, 220, 0, 245, 0, 255, 0, 255, 0,
        )
        private const val RAMP_AMPLITUDE_REPEAT = 16

        // Ramp, duration-only path (no-amplitude waveform + legacy): leading 0 is the initial off
        // delay, so the loop index shifts to 17.
        private val RAMP_PATTERN = longArrayOf(
            0, 20, 140, 25, 110, 30, 85, 35, 65, 40, 45, 50, 30, 60, 18, 70, 12, 90, 10,
        )
        private const val RAMP_PATTERN_REPEAT = 17

        // Success "dot-daaat": a short medium pulse, a gap, then a longer full-strength pulse.
        private val SUCCESS_AMPLITUDE_TIMINGS = longArrayOf(40, 90, 200)
        private val SUCCESS_AMPLITUDES = intArrayOf(200, 0, 255)
        private val SUCCESS_PATTERN = longArrayOf(0, 40, 90, 200)

        // Error "dot-dot": two equal short pulses.
        private val ERROR_AMPLITUDE_TIMINGS = longArrayOf(60, 100, 60)
        private val ERROR_AMPLITUDES = intArrayOf(255, 0, 255)
        private val ERROR_PATTERN = longArrayOf(0, 60, 100, 60)

        // Neutral: a single soft pulse.
        private val NEUTRAL_AMPLITUDE_TIMINGS = longArrayOf(90)
        private val NEUTRAL_AMPLITUDES = intArrayOf(120)
        private val NEUTRAL_PATTERN = longArrayOf(0, 90)
    }
}
