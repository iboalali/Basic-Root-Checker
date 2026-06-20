package com.iboalali.basicrootchecker.review

import androidx.activity.ComponentActivity

/**
 * Drives the in-app rating prompt. The Google Play implementation shows the Play-managed in-app
 * review card; the FOSS implementation is a no-op. Mirrors the [AppUpdateController] flavor split.
 *
 * The managed card is quota-limited by Play and gives no "was it shown" callback, so callers gate
 * *whether* to request it (see the gating in `MainViewModel`) and never rely on it actually
 * appearing.
 */
interface ReviewController {

    /**
     * Whether in-app rating is supported in this build (true on Google Play, false on FOSS). Used to
     * hide the explicit "Rate this app" entry where there's no Play Store to rate on.
     */
    val isAvailable: Boolean

    /** Binds to [activity] so the review flow can be launched; cleared automatically on destroy. */
    fun attach(activity: ComponentActivity)

    /** Requests the Play-managed in-app review flow. No-op if unattached or unavailable. */
    fun requestReview()
}
