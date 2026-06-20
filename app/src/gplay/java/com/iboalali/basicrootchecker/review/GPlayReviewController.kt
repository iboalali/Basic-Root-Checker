package com.iboalali.basicrootchecker.review

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.iboalali.basicrootchecker.analytics.Analytics

private const val TAG = "GPlayReview"

class GPlayReviewController(context: Context) : ReviewController {

    private val reviewManager: ReviewManager = ReviewManagerFactory.create(context)

    override val isAvailable: Boolean = true

    private var activity: ComponentActivity? = null

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            detach()
        }
    }

    override fun attach(activity: ComponentActivity) {
        if (this.activity === activity) return
        if (this.activity != null) detach()

        this.activity = activity
        activity.lifecycle.addObserver(lifecycleObserver)
    }

    private fun detach() {
        activity?.lifecycle?.removeObserver(lifecycleObserver)
        activity = null
    }

    override fun requestReview() {
        val activity = activity ?: return
        reviewManager.requestReviewFlow()
            .addOnSuccessListener { reviewInfo ->
                reviewManager.launchReviewFlow(activity, reviewInfo)
                    .addOnFailureListener { e ->
                        Log.w(TAG, "launchReviewFlow failed", e)
                        Analytics.trackReviewFlowFailed(e.formatReviewError())
                    }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "requestReviewFlow failed", e)
                Analytics.trackReviewFlowFailed(e.formatReviewError())
            }
    }

    private fun Exception.formatReviewError(): String =
        "${this::class.simpleName ?: "Exception"}: ${message ?: "unknown"}"
}
