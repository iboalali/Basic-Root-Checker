package com.iboalali.basicrootchecker.review

import androidx.activity.ComponentActivity

object NoOpReviewController : ReviewController {
    override val isAvailable: Boolean = false

    override fun attach(activity: ComponentActivity) = Unit

    /** Never requests anything, so callers keep their prompt slot unspent. */
    override fun requestReview(): Boolean = false
}
