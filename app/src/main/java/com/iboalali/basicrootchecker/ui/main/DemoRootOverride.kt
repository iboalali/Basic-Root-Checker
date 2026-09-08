package com.iboalali.basicrootchecker.ui.main

import android.content.Intent
import android.util.Log
import com.iboalali.basicrootchecker.data.RootManager
import com.iboalali.basicrootchecker.data.RootProvider
import com.iboalali.basicrootchecker.data.RootResult

/**
 * Debug-only: a [RootResult] armed from outside the app, so the check can be driven to a known
 * outcome with nothing on screen to show for it.
 *
 * [DebugRootResultDialog] already forces a result, but it is a picker somebody taps: right for
 * exercising animations by hand, useless for the store video, where a developer dialog would sit in
 * the middle of the shot. Arming instead leaves the FAB behaving exactly as it does in a release
 * build (checking snackbar, spinner, result animation, outcome haptic); only the answer is decided
 * in advance.
 *
 *     adb shell am start -n com.iboalali.basicrootchecker.debug/com.iboalali.basicrootchecker.MainActivity \
 *       --es demo_root magisk --es demo_root_version 29.0
 *
 * Read once in `MainActivity.onCreate`, so **each cold start decides**: launching without the extra
 * disarms, and there is no state to reset between runs. A warm start does not re-arm. The recorder
 * force-stops the app before every pass anyway, and the alternative (an `onNewIntent` hook) would
 * make the armed result depend on whether the task happened to still be alive.
 *
 * The vocabulary deliberately mirrors [DebugRootResultDialog]'s rows rather than inventing a second
 * one, so a result seen by hand can be reproduced from a script.
 *
 * The only call sites are behind `BuildConfig.DEBUG`, so this is unreachable in release and
 * stripped with the rest of the debug tooling.
 */
object DemoRootOverride {

    const val EXTRA_RESULT: String = "demo_root"
    const val EXTRA_VERSION: String = "demo_root_version"

    /** The version reported for Magisk-family results when the intent does not name one. */
    private const val DEFAULT_MAGISK_VERSION = "29.0"

    @Volatile
    var armed: RootResult? = null
        private set

    /**
     * Arm (or disarm) from a launch intent. Absent extra disarms; an unknown token disarms loudly.
     */
    fun applyFrom(intent: Intent?) {
        val token = intent?.getStringExtra(EXTRA_RESULT)?.trim()?.lowercase()
        if (token.isNullOrEmpty()) {
            armed = null
            return
        }
        val version = intent.getStringExtra(EXTRA_VERSION)?.trim()?.takeIf { it.isNotEmpty() }
        val parsed = parse(token, version)
        if (parsed == null) {
            // Loudly, because the failure mode is otherwise silent: the app comes up
            // looking correct and the check simply runs for real, which on an unrooted
            // device reads as a broken demo rather than as a typo in the token.
            Log.w(TAG, "unknown $EXTRA_RESULT value '$token'; running the real check instead")
        }
        armed = parsed
    }

    private fun parse(token: String, version: String?): RootResult? =
        when (token) {
            "magisk" ->
                RootResult.Rooted(
                    RootProvider.MAGISK,
                    RootManager.MAGISK,
                    version ?: DEFAULT_MAGISK_VERSION,
                )
            "kitsune" ->
                RootResult.Rooted(
                    RootProvider.MAGISK,
                    RootManager.KITSUNE_MASK,
                    version ?: DEFAULT_MAGISK_VERSION,
                )
            "kernelsu" -> RootResult.Rooted(RootProvider.KERNELSU, RootManager.KERNELSU, null)
            "sukisu" -> RootResult.Rooted(RootProvider.KERNELSU, RootManager.SUKISU_ULTRA, null)
            "apatch" -> RootResult.Rooted(RootProvider.APATCH, RootManager.APATCH, null)
            "not-granted" -> RootResult.RootedNotGranted(RootProvider.MAGISK, RootManager.MAGISK)
            "not-rooted" -> RootResult.NotRooted
            "unknown" -> RootResult.Unknown
            else -> null
        }

    private const val TAG = "DemoRootOverride"
}
