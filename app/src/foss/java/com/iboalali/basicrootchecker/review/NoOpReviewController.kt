package com.iboalali.basicrootchecker.review

import androidx.activity.ComponentActivity

object NoOpReviewController : ReviewController {
    override val isAvailable: Boolean = false

    override fun attach(activity: ComponentActivity) = Unit
    override fun requestReview() = Unit
}
