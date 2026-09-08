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

    /**
     * Requests the Play-managed in-app review flow.
     *
     * @return `true` when the request was actually handed to Play, `false` when it could not be —
     *   [isAvailable] is false, or no activity is attached (e.g. a root check that finished while
     *   the activity was being recreated). Callers use this to avoid spending their
     *   once-per-version prompt slot on a flow that never ran; a `true` return still says nothing
     *   about whether Play went on to *show* the card.
     */
    fun requestReview(): Boolean
}
