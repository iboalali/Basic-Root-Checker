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
        val primitiveClick =
            v != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)
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
                "primitiveClick=$primitiveClick envelopeEffectsSupported=$envelopeSupported " +
                "amplitudeControl=$amplitudeControl freqProfile=$freqProfile",
        )
        Analytics.trackHapticCapabilities(hasVibrator, primitiveClick, envelopeSupported, amplitudeControl, sdk)
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

    /** A subtle one-shot for ordinary taps and toggles. */
    fun playTap() = playOneShot("haptic-tap", TAP_PRIMITIVE_SCALE, TAP_MS, TAP_AMPLITUDE)

    /** A slightly firmer one-shot for long-press confirmations (e.g. copy on long-press). */
    fun playLongPress() =
        playOneShot("haptic-longpress", LONG_PRESS_PRIMITIVE_SCALE, LONG_PRESS_MS, LONG_PRESS_AMPLITUDE)

    /**
     * Plays a short one-shot for UI feedback (taps, long-press). Driven through the [Vibrator]
     * directly (the same path the root-check buzz uses) rather than `View.performHapticFeedback`,
     * because many OEMs (Samsung, OnePlus, Xiaomi, Vivo) silently drop the framework tap haptics;
     * the Vibrator works regardless, gated only by the in-app toggle and the master vibration
     * setting. Unlike [play] it does *not* cancel an in-flight vibration, so a tap can't cut off a
     * root-check buzz.
     *
     * Two tiers only, both raw enough to actually fire across OEMs: a click primitive where the
     * actuator genuinely supports it ([Vibrator.areAllPrimitivesSupported], API 30+ — the crispest,
     * e.g. Pixel), otherwise a raw amplitude [VibrationEffect.createOneShot] (API 26+) / legacy
     * duration vibrate (API 23-25). We deliberately skip `createPredefined` (`EFFECT_CLICK` etc.):
     * like the framework constants, predefined effects are HAL-mapped and several OEMs (notably
     * Samsung) silently drop them, whereas a raw amplitude pulse always reaches the motor.
     */
    private fun playOneShot(id: String, primitiveScale: Float, oneShotMs: Long, oneShotAmplitude: Int) {
        val v = vibrator ?: return
        if (!available) return
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK) ->
                    v.vibrate(
                        VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, primitiveScale)
                            .compose(),
                    )

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    val amplitude =
                        if (v.hasAmplitudeControl()) oneShotAmplitude else VibrationEffect.DEFAULT_AMPLITUDE
                    v.vibrate(VibrationEffect.createOneShot(oneShotMs, amplitude))
                }

                else -> {
                    @Suppress("DEPRECATION")
                    v.vibrate(oneShotMs)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "playOneShot $id: ", e)
            Analytics.trackError(e, id = id, category = ERROR_CATEGORY_APP_STATE)
        }
    }

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

        // Subtle UI one-shots. PRIMITIVE_CLICK scale (API 30+, e.g. Pixel) keeps the tap lighter
        // than the long-press. The ms/amplitude pairs are the raw-amplitude fallback for devices
        // without primitive support (e.g. Samsung) — short, but long/strong enough to actually be
        // felt on a typical LRA; out of 255 amplitude.
        private const val TAP_PRIMITIVE_SCALE = 0.6f
        private const val LONG_PRESS_PRIMITIVE_SCALE = 1f
        private const val TAP_MS = 25L
        private const val TAP_AMPLITUDE = 120
        private const val LONG_PRESS_MS = 45L
        private const val LONG_PRESS_AMPLITUDE = 210
    }
}
