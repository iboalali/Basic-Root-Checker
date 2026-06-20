package com.iboalali.basicrootchecker

import android.app.Application
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.billing.BillingController
import com.iboalali.basicrootchecker.billing.createBillingController
import com.iboalali.basicrootchecker.data.UserPreferences
import com.iboalali.basicrootchecker.review.ReviewController
import com.iboalali.basicrootchecker.review.createReviewController
import com.iboalali.basicrootchecker.update.AppUpdateController
import com.iboalali.basicrootchecker.update.createAppUpdateController
import com.iboalali.basicrootchecker.util.RootHaptics
import com.telemetrydeck.sdk.TelemetryDeck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BasicRootCheckerApplication : Application() {

    val appUpdateController: AppUpdateController by lazy { createAppUpdateController(this) }

    val billingController: BillingController by lazy { createBillingController(this) }

    val reviewController: ReviewController by lazy { createReviewController(this) }

    /** Single app-wide haptics engine, shared by the root-check flow and the UI tap feedback. */
    val rootHaptics: RootHaptics by lazy { RootHaptics(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Register synchronously so crashes during the async init below are still captured
        // (buffered by Analytics until the opt-out preference is resolved).
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Analytics.trackError(throwable)
            previous?.uncaughtException(thread, throwable)
        }

        // Read the opt-out preference off the main thread so the (potentially slow) DataStore
        // read never blocks cold start. TelemetryDeck.start() registers a lifecycle observer
        // and must run on the main thread, so we hop back for just that call — it's posted to
        // the main queue and runs after the first frame rather than blocking onCreate. Signals
        // fired in the meantime are buffered by Analytics; setEnabled() then flushes them
        // (enabled, after start()) or discards them (disabled).
        applicationScope.launch {
            val enabled = runCatching {
                UserPreferences(this@BasicRootCheckerApplication).telemetryEnabled.first()
            }.getOrDefault(false)
            if (enabled) {
                withContext(Dispatchers.Main) {
                    val builder = TelemetryDeck.Builder()
                        .appID(BuildConfig.TELEMETRY_DECK_APP_ID)
                        .showDebugLogs(BuildConfig.DEBUG)
                        .testMode(BuildConfig.DEBUG)
                    TelemetryDeck.start(applicationContext, builder)
                }
            }
            Analytics.setEnabled(enabled)
        }
    }
}
