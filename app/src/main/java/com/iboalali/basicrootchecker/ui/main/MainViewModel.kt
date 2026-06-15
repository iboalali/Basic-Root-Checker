package com.iboalali.basicrootchecker.ui.main

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iboalali.basicrootchecker.BasicRootCheckerApplication
import com.iboalali.basicrootchecker.BuildConfig
import com.iboalali.basicrootchecker.R
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.data.RootChecker
import com.iboalali.basicrootchecker.data.RootManager
import com.iboalali.basicrootchecker.data.RootProvider
import com.iboalali.basicrootchecker.data.RootResult
import com.iboalali.basicrootchecker.data.UserPreferences
import com.iboalali.basicrootchecker.update.AppUpdateEvent
import com.iboalali.basicrootchecker.util.DeviceInfo
import com.iboalali.basicrootchecker.util.RootHaptics
import de.boehrsi.devicemarketingnames.DeviceMarketingNames
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RootStatus {
    NOT_CHECKED,
    CHECKING,
    ROOTED,
    NOT_ROOTED,
    UNKNOWN,
    NOT_GRANTED,
}

data class MainUiState(
    val rootStatus: RootStatus = RootStatus.NOT_CHECKED,
    val rootProvider: RootProvider = RootProvider.UNKNOWN,
    val rootManager: RootManager? = null,
    val rootProviderVersion: String? = null,
    val deviceMarketingName: String = "",
    val deviceModelName: String = "",
    val androidVersion: String = "",
    val updateStatus: AppUpdateEvent = AppUpdateEvent.None,
    val appUpdatedShown: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appUpdateController =
        (application as BasicRootCheckerApplication).appUpdateController

    private val userPreferences = UserPreferences(application)

    private val haptics = RootHaptics(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadDeviceInfo()
        viewModelScope.launch {
            appUpdateController.events.collect { event ->
                if (!demoUpdateActive) _uiState.update { it.copy(updateStatus = event) }
            }
        }
        viewModelScope.launch { checkForAppUpdate() }
    }

    private suspend fun checkForAppUpdate() {
        val stored = userPreferences.lastSeenVersionCode.first()
        val current = BuildConfig.VERSION_CODE
        if (stored in 1..<current) {
            _uiState.update { it.copy(appUpdatedShown = true) }
        }
        if (stored != current) {
            userPreferences.setLastSeenVersionCode(current)
        }
    }

    fun onAppUpdatedSnackbarShown() {
        _uiState.update { it.copy(appUpdatedShown = false) }
    }

    private fun loadDeviceInfo() {
        val app = getApplication<Application>()
        val resources = app.resources

        _uiState.update {
            it.copy(
                deviceMarketingName = DeviceMarketingNames.getSingleName(),
                deviceModelName = Build.DEVICE,
                androidVersion = "${resources.getString(R.string.textViewAndroidVersion)} ${DeviceInfo.getAndroidVersionName()}",
            )
        }
    }

    fun checkRoot() {
        viewModelScope.launch {
            val hapticsOn = userPreferences.hapticsEnabled.first()
            startRootCheck()
            if (hapticsOn) haptics.startCheckingRamp()
            Analytics.trackRootCheckStarted()
            val result = RootChecker.check(getApplication())
            applyResult(result)
            if (hapticsOn) playResultHaptic(result)
        }
    }

    fun requestRoot() {
        viewModelScope.launch {
            val hapticsOn = userPreferences.hapticsEnabled.first()
            startRootCheck()
            if (hapticsOn) haptics.startCheckingRamp()
            Analytics.trackRootRequested()
            val result = RootChecker.requestRoot(getApplication())
            applyResult(result)
            if (hapticsOn) playResultHaptic(result)
        }
    }

    private fun playResultHaptic(result: RootResult) = when (result) {
        is RootResult.Rooted -> haptics.playSuccess()
        RootResult.NotRooted, RootResult.Unknown -> haptics.playError()
        is RootResult.RootedNotGranted -> haptics.playNeutral()
    }

    /**
     * Debug-only: forces [result] through the same flow a real check uses (CHECKING state, haptic
     * ramp, ~1s delay, then result + outcome haptic) so the animations and haptics can be exercised
     * on-device without a matching root state. Only ever called from the debug-gated demo dialog.
     */
    fun checkRootDemo(result: RootResult) {
        viewModelScope.launch {
            val hapticsOn = userPreferences.hapticsEnabled.first()
            startRootCheck()
            if (hapticsOn) haptics.startCheckingRamp()
            delay(1000)
            applyResult(result)
            if (hapticsOn) playResultHaptic(result)
        }
    }

    private fun startRootCheck() {
        _uiState.update {
            it.copy(
                rootStatus = RootStatus.CHECKING,
                rootProvider = RootProvider.UNKNOWN,
                rootManager = null,
                rootProviderVersion = null,
            )
        }
    }

    private data class ResolvedRoot(
        val status: RootStatus,
        val provider: RootProvider,
        val manager: RootManager?,
        val version: String?,
    )

    private fun applyResult(result: RootResult) {
        val resolved = when (result) {
            is RootResult.Rooted ->
                ResolvedRoot(RootStatus.ROOTED, result.provider, result.manager, result.version)
            RootResult.NotRooted -> ResolvedRoot(RootStatus.NOT_ROOTED, RootProvider.UNKNOWN, null, null)
            RootResult.Unknown -> ResolvedRoot(RootStatus.UNKNOWN, RootProvider.UNKNOWN, null, null)
            is RootResult.RootedNotGranted ->
                ResolvedRoot(RootStatus.NOT_GRANTED, result.provider, result.manager, null)
        }
        _uiState.update {
            it.copy(
                rootStatus = resolved.status,
                rootProvider = resolved.provider,
                rootManager = resolved.manager,
                rootProviderVersion = resolved.version,
            )
        }
        Analytics.trackRootCheckResult(resolved.status.name)
        if (resolved.status == RootStatus.ROOTED) {
            Analytics.trackRootProvider(resolved.provider.name, resolved.manager?.name, resolved.version)
        }
    }

    fun onUpdateRequested() {
        if (demoUpdateActive) {
            demoUpdateDownloading()
            return
        }
        appUpdateController.startFlexibleFlow()
    }

    fun onInstallRequested() {
        if (demoUpdateActive) {
            // Can't actually restart into a fake update — just hide the card.
            demoUpdate(AppUpdateEvent.None)
            return
        }
        appUpdateController.completeUpdate()
    }

    // ---- Debug-only in-app-update demo ----
    // Pushes fake AppUpdateEvents straight into updateStatus, bypassing the Play controller, so the
    // UpdateCard's states and download animation can be exercised without a real Play update. Only
    // ever reached from the debug-gated demo dialog and the demo-aware buttons above.
    private var demoUpdateActive = false
    private var demoUpdateJob: Job? = null

    fun demoUpdate(event: AppUpdateEvent) {
        demoUpdateJob?.cancel()
        demoUpdateActive = event != AppUpdateEvent.None
        _uiState.update { it.copy(updateStatus = event) }
    }

    fun demoUpdateDownloading() {
        demoUpdateJob?.cancel()
        demoUpdateActive = true
        demoUpdateJob = viewModelScope.launch {
            val total = 18L * 1024 * 1024
            val step = total / 24
            var downloaded = 0L
            while (downloaded < total) {
                _uiState.update {
                    it.copy(updateStatus = AppUpdateEvent.Downloading(downloaded, total))
                }
                delay(120)
                downloaded += step
            }
            _uiState.update { it.copy(updateStatus = AppUpdateEvent.Downloaded) }
        }
    }

    override fun onCleared() {
        haptics.cancel()
    }
}
