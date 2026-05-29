package com.iboalali.basicrootchecker.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.analytics.ERROR_CATEGORY_APP_STATE

/**
 * Plays the vibration feedback that accompanies a root check.
 *
 * The [Vibrator] is resolved once. The checking ramp is a genuine rising-frequency sweep that only
 * plays on actuators supporting wave-envelope (PWLE) effects (API 36+ and HAL support — rare
 * today); every other device gets no checking vibration. Once the result is known, every device
 * feels a short outcome buzz (amplitude-controlled waveform on API 26+, or the legacy pattern API
 * on API 23-25).
 */
class RootHaptics(context: Context) {

    private val vibrator: Vibrator? = resolveVibrator(context)

    private val available: Boolean
        get() = vibrator?.hasVibrator() == true

    init {
        logAndTrackCapabilities()
    }

    /** Logs and reports (once, on creation) whether this device can do the advanced sweep. */
    private fun logAndTrackCapabilities() {
        val v = vibrator
        val sdk = Build.VERSION.SDK_INT
        val hasVibrator = v?.hasVibrator() == true
        val amplitudeControl =
            v != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && v.hasAmplitudeControl()
        var envelopeSupported = false
        var freqProfile = "n/a"
        if (v != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            envelopeSupported = v.areEnvelopeEffectsSupported()
            val profile = v.frequencyProfile
            if (profile != null) {
                freqProfile = "${profile.minFrequencyHz}-${profile.maxFrequencyHz}Hz"
            }
        }
        Log.i(
            TAG,
            "capabilities: sdk=$sdk hasVibrator=$hasVibrator " +
                "envelopeEffectsSupported=$envelopeSupported " +
                "amplitudeControl=$amplitudeControl freqProfile=$freqProfile",
        )
        Analytics.trackHapticCapabilities(envelopeSupported, amplitudeControl, sdk)
    }

    /**
     * Starts one continuous buzz that gently builds in amplitude while rising in frequency, until
     * [cancel] is called. This only plays on actuators that support envelope effects (API 36+);
     * other devices get no checking ramp and feel only the result buzz.
     */
    fun startCheckingRamp() {
        val v = vibrator ?: return
        if (!available) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
            Log.i(TAG, "startCheckingRamp: no wave-envelope API (SDK ${Build.VERSION.SDK_INT}) — result buzz only")
            return
        }
        startSweep(v)
    }

    /**
     * Plays a genuine rising-frequency sweep via the wave-envelope ([VibrationEffect]
     * [VibrationEffect.WaveformEnvelopeBuilder]) API, sweeping the actuator's full supported
     * frequency range at building amplitude. Only actuators that support envelope effects (PWLE) get
     * any checking vibration; everything else falls through to just the result buzz.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun startSweep(v: Vibrator) {
        if (!v.areEnvelopeEffectsSupported()) {
            Log.i(TAG, "startCheckingRamp: wave envelope unsupported — result buzz only")
            return
        }
        val profile = v.frequencyProfile
        val low = profile?.minFrequencyHz ?: Float.NaN
        val high = profile?.maxFrequencyHz ?: Float.NaN
        if (!low.isFinite() || !high.isFinite() || high <= low || low <= 0f) {
            Log.i(TAG, "startCheckingRamp: no usable frequency profile ($low..$high) — result buzz only")
            return
        }
        try {
            Log.i(TAG, "startCheckingRamp: wave-envelope frequency sweep $low->$high Hz")
            // Soft attack at the low frequency, then build amplitude while sweeping up to the high
            // frequency, and a short release at the top.
            val effect = VibrationEffect.WaveformEnvelopeBuilder()
                .addControlPoint(RAMP_START_AMPLITUDE, low, RAMP_ATTACK_MS)
                .addControlPoint(1f, high, RAMP_SWEEP_MS)
                .addControlPoint(0f, high, RAMP_RELEASE_MS)
                .build()
            v.cancel()
            v.vibrate(effect)
        } catch (e: Exception) {
            Log.e(TAG, "startSweep: ", e)
            Analytics.trackError(e, id = "haptic-ramp", category = ERROR_CATEGORY_APP_STATE)
        }
    }

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

        // Wave-envelope frequency sweep (envelope-capable devices only): soft attack at the low
        // frequency, then build amplitude while sweeping up to the high frequency, short release.
        private const val RAMP_START_AMPLITUDE = 0.4f
        private const val RAMP_ATTACK_MS = 60L
        private const val RAMP_SWEEP_MS = 900L
        private const val RAMP_RELEASE_MS = 40L

        // Success "dot-daaat": a short medium pulse, a gap, then a longer full-strength pulse.
        private val SUCCESS_AMPLITUDE_TIMINGS = longArrayOf(90, 50, 200)
        private val SUCCESS_AMPLITUDES = intArrayOf(150, 0, 255)
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
