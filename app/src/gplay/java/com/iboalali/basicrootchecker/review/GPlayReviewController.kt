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

    override fun requestReview(): Boolean {
        // No activity bound (e.g. mid-recreation): report it so the caller keeps its prompt slot
        // instead of marking this version as already asked.
        val activity = activity ?: return false
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
        // The flow is now Play's to run: it resolves asynchronously and gives no "was it shown"
        // callback, so this only reports that the request was made, not that a card appeared.
        return true
    }

    private fun Exception.formatReviewError(): String =
        "${this::class.simpleName ?: "Exception"}: ${message ?: "unknown"}"
}
