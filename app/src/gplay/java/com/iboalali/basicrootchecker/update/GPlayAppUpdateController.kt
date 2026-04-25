package com.iboalali.basicrootchecker.update

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallErrorCode
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.iboalali.basicrootchecker.analytics.Analytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "GPlayAppUpdate"
private const val STALENESS_DAYS_THRESHOLD = 1

class GPlayAppUpdateController(context: Context) : AppUpdateController {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)

    private val _events = MutableStateFlow<AppUpdateEvent>(AppUpdateEvent.None)
    override val events: StateFlow<AppUpdateEvent> = _events.asStateFlow()

    private var activity: ComponentActivity? = null
    private var launcher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var latestUpdateInfo: AppUpdateInfo? = null

    private val installStateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                _events.value = AppUpdateEvent.Downloading(
                    state.bytesDownloaded(),
                    state.totalBytesToDownload(),
                )
            }
            InstallStatus.DOWNLOADED -> {
                if (_events.value !is AppUpdateEvent.Downloaded) {
                    Analytics.trackUpdateDownloaded()
                }
                _events.value = AppUpdateEvent.Downloaded
            }
            InstallStatus.FAILED -> {
                val code = state.installErrorCode()
                _events.value = AppUpdateEvent.Failed(code)
                Analytics.trackUpdateFailed(formatInstallError(code))
            }
            else -> Unit
        }
    }

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            appUpdateManager.registerListener(installStateListener)
        }

        override fun onResume(owner: LifecycleOwner) {
            checkForUpdate()
        }

        override fun onStop(owner: LifecycleOwner) {
            appUpdateManager.unregisterListener(installStateListener)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            detach()
        }
    }

    override fun attach(activity: ComponentActivity) {
        if (this.activity === activity) return
        if (this.activity != null) detach()

        this.activity = activity
        launcher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                Log.w(TAG, "Update flow cancelled or failed: resultCode=${result.resultCode}")
            }
        }
        activity.lifecycle.addObserver(lifecycleObserver)
    }

    private fun detach() {
        activity?.lifecycle?.removeObserver(lifecycleObserver)
        runCatching { appUpdateManager.unregisterListener(installStateListener) }
        activity = null
        launcher = null
        latestUpdateInfo = null
    }

    override fun checkForUpdate() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                latestUpdateInfo = info
                val current = _events.value
                if (current is AppUpdateEvent.Downloading) return@addOnSuccessListener

                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    _events.value = AppUpdateEvent.Downloaded
                    return@addOnSuccessListener
                }

                val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                val flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                val stale = (info.clientVersionStalenessDays() ?: -1) >= STALENESS_DAYS_THRESHOLD

                if (available && flexibleAllowed && stale) {
                    if (current !is AppUpdateEvent.Available) {
                        Analytics.trackUpdateAvailable()
                    }
                    _events.value = AppUpdateEvent.Available
                } else if (current is AppUpdateEvent.Available) {
                    _events.value = AppUpdateEvent.None
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "requestAppUpdateInfo failed", e)
            }
    }

    override fun startFlexibleFlow() {
        val info = latestUpdateInfo ?: return
        val l = launcher ?: return
        try {
            val started = appUpdateManager.startUpdateFlowForResult(
                info,
                l,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
            )
            if (started) {
                Analytics.trackUpdateStarted()
                _events.value = AppUpdateEvent.Downloading(0, 0)
            }
        } catch (e: IntentSender.SendIntentException) {
            Log.w(TAG, "startUpdateFlowForResult failed", e)
        }
    }

    override fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    private fun formatInstallError(code: Int): String {
        val name = when (code) {
            InstallErrorCode.NO_ERROR -> "NO_ERROR"
            InstallErrorCode.ERROR_UNKNOWN -> "ERROR_UNKNOWN"
            InstallErrorCode.ERROR_API_NOT_AVAILABLE -> "ERROR_API_NOT_AVAILABLE"
            InstallErrorCode.ERROR_INVALID_REQUEST -> "ERROR_INVALID_REQUEST"
            InstallErrorCode.ERROR_INSTALL_UNAVAILABLE -> "ERROR_INSTALL_UNAVAILABLE"
            InstallErrorCode.ERROR_INSTALL_NOT_ALLOWED -> "ERROR_INSTALL_NOT_ALLOWED"
            InstallErrorCode.ERROR_DOWNLOAD_NOT_PRESENT -> "ERROR_DOWNLOAD_NOT_PRESENT"
            InstallErrorCode.ERROR_INTERNAL_ERROR -> "ERROR_INTERNAL_ERROR"
            InstallErrorCode.ERROR_PLAY_STORE_NOT_FOUND -> "ERROR_PLAY_STORE_NOT_FOUND"
            InstallErrorCode.ERROR_APP_NOT_OWNED -> "ERROR_APP_NOT_OWNED"
            else -> "ERROR_UNMAPPED"
        }
        return "$name ($code)"
    }
}
