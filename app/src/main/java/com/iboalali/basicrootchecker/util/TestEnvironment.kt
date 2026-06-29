package com.iboalali.basicrootchecker.util

import android.content.Context
import android.provider.Settings
import android.util.Log

/**
 * Detects synthetic, non-user runs so their telemetry can be flagged as test-mode rather than
 * counted as real usage.
 *
 * Google's CI infrastructure — **Firebase Test Lab** and the **Play Console pre-launch report**
 * robot (which runs on Test Lab) — both set the documented `firebase.test.lab` system setting to
 * `"true"`. That traffic otherwise inflates every production metric (installs, sessions, root
 * checks) and shows up as bogus devices/packages.
 *
 * We flag it as **test-mode** rather than dropping it client-side: a TelemetryDeck test-mode signal
 * is globally segregated out of the production view (including the premade dashboards) yet remains
 * inspectable via the dashboard's Test Mode toggle. The trade-off is deliberate — a detection
 * false-positive merely misfiles a real user's data into the test bucket (recoverable), whereas
 * dropping would silently and permanently destroy it. (TelemetryDeck has no global/app-level query
 * filter — filtering is per-insight only and the premade dashboards can't be filtered at all — so
 * this has to be decided client-side at signal time.)
 */
object TestEnvironment {

    /**
     * `true` when the app is running under Firebase Test Lab or the Play Console pre-launch report.
     *
     * **Fail-open:** any failure reading the setting returns `false`, so an unexpected device that
     * can't be probed is treated as a real user (it keeps its analytics) rather than misfiled as a
     * test run.
     */
    fun isFirebaseTestLab(context: Context): Boolean =
        try {
            Settings.System.getString(context.contentResolver, "firebase.test.lab") == "true"
        } catch (e: Exception) {
            Log.w(TAG, "isFirebaseTestLab: ", e)
            false
        }

    private const val TAG = "TestEnvironment"
}
