package com.iboalali.basicrootchecker.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/**
 * Opens the Play Store listing for [packageName] (this app by default), preferring the Play Store
 * app via the `market://` scheme and falling back to the web listing when it isn't installed.
 */
fun Context.openPlayStoreListing(packageName: String = this.packageName) {
    try {
        startActivity(
            Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
        )
    } catch (_: ActivityNotFoundException) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=$packageName".toUri()
            )
        )
    }
}
