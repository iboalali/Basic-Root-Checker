package com.iboalali.basicrootchecker.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.analytics.ERROR_CATEGORY_APP_STATE

private const val TAG = "PlayStore"

/**
 * Opens the Play Store listing for [packageName] (this app by default), preferring the Play Store app
 * via the `market://` scheme and falling back to the web listing when it isn't installed.
 *
 * Both attempts are guarded: the web fallback needs a browser, and a device can have neither (a
 * stripped ROM, a kiosk/managed device, a FOSS build on hardware with no Play services and no
 * browser). Rather than throw an unhandled [ActivityNotFoundException] out of a click handler, this
 * gives up quietly and reports it — the caller is always an optional "rate"/"install" affordance, so
 * there is nothing the user needs to be interrupted about.
 */
fun Context.openPlayStoreListing(packageName: String = this.packageName) {
    if (startViewIntent("market://details?id=$packageName")) return
    if (startViewIntent("https://play.google.com/store/apps/details?id=$packageName")) return
    Log.w(TAG, "No activity could open the Play Store listing for $packageName")
    Analytics.trackError(
        id = "openPlayStoreListing",
        message = "No Play Store app and no browser",
        category = ERROR_CATEGORY_APP_STATE,
    )
}

/** Starts an `ACTION_VIEW` for [uri], returning false when nothing on the device handles it. */
private fun Context.startViewIntent(uri: String): Boolean =
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
        true
    } catch (e: ActivityNotFoundException) {
        Log.i(TAG, "No activity for $uri", e)
        false
    }
