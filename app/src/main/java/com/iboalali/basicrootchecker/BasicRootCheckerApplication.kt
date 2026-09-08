package com.iboalali.basicrootchecker

import android.app.Application
import android.util.Log
import com.iboalali.appcatalog.data.AppCatalogRepository
import com.iboalali.appcatalog.data.CatalogAnalytics
import com.iboalali.appcatalog.data.CatalogLogLevel
import com.iboalali.appcatalog.data.CatalogLogger
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.billing.BillingController
import com.iboalali.basicrootchecker.billing.createBillingController
import com.iboalali.basicrootchecker.data.UserPreferences
import com.iboalali.basicrootchecker.review.ReviewController
import com.iboalali.basicrootchecker.review.createReviewController
import com.iboalali.basicrootchecker.update.AppUpdateController
import com.iboalali.basicrootchecker.update.createAppUpdateController
import com.iboalali.basicrootchecker.util.RootHaptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BasicRootCheckerApplication : Application() {

    private companion object {
        const val CATALOG_LOG_TAG = "AppCatalogRepository"
    }

    val appUpdateController: AppUpdateController by lazy { createAppUpdateController(this) }

    val billingController: BillingController by lazy { createBillingController(this) }

    val reviewController: ReviewController by lazy { createReviewController(this) }

    /** Single app-wide haptics engine, shared by the root-check flow and the UI tap feedback. */
    val rootHaptics: RootHaptics by lazy { RootHaptics(this) }

    /**
     * Source of truth for the About screen's "Other apps" list. Seeds from cache/bundled snapshot
     * on first access; `MainActivity` calls [AppCatalogRepository.refresh] once at app start to
     * revalidate it in the background.
     */
    val appCatalogRepository: AppCatalogRepository by lazy {
        AppCatalogRepository(
            context = this,
            analytics =
                CatalogAnalytics { result, error ->
                    Analytics.trackAppCatalogRefresh(result, error)
                },
            logger =
                CatalogLogger { level, throwable, message ->
                    when (level) {
                        CatalogLogLevel.INFO -> Log.i(CATALOG_LOG_TAG, message, throwable)
                        CatalogLogLevel.WARN -> Log.w(CATALOG_LOG_TAG, message, throwable)
                    }
                },
        )
    }

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

        // Read the opt-out preference off the main thread, so the (potentially slow) DataStore read
        // never blocks cold start; then hop back to Main to apply it, because TelemetryDeck.start()
        // registers a lifecycle observer. That hop is posted to the main queue, so it runs after
        // the first frame rather than inside onCreate.
        //
        // Signals fired in the meantime are buffered. resolveStartupPreference() starts the SDK and
        // then releases them, or discards them if the user opted out. Start-before-flush is the
        // shared controller's guarantee now rather than this call site's — see
        // com.iboalali.telemetry.TelemetryController, where a test covers it.
        applicationScope.launch {
            val enabled =
                runCatching {
                        UserPreferences(this@BasicRootCheckerApplication).telemetryEnabled.first()
                    }
                    .getOrDefault(false)
            withContext(Dispatchers.Main) {
                Analytics.resolveStartupPreference(applicationContext, enabled)
            }
        }
    }
}
