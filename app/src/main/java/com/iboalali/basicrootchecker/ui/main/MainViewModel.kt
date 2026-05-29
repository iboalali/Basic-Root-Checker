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
import com.iboalali.basicrootchecker.data.RootProvider
import com.iboalali.basicrootchecker.data.RootResult
import com.iboalali.basicrootchecker.data.UserPreferences
import com.iboalali.basicrootchecker.update.AppUpdateEvent
import com.iboalali.basicrootchecker.util.DeviceInfo
import com.iboalali.basicrootchecker.util.RootHaptics
import de.boehrsi.devicemarketingnames.DeviceMarketingNames
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
                _uiState.update { it.copy(updateStatus = event) }
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

    private fun startRootCheck() {
        _uiState.update {
            it.copy(
                rootStatus = RootStatus.CHECKING,
                rootProvider = RootProvider.UNKNOWN,
                rootProviderVersion = null,
            )
        }
    }

    private data class ResolvedRoot(
        val status: RootStatus,
        val provider: RootProvider,
        val version: String?,
    )

    private fun applyResult(result: RootResult) {
        val resolved = when (result) {
            is RootResult.Rooted -> ResolvedRoot(RootStatus.ROOTED, result.provider, result.version)
            RootResult.NotRooted -> ResolvedRoot(RootStatus.NOT_ROOTED, RootProvider.UNKNOWN, null)
            RootResult.Unknown -> ResolvedRoot(RootStatus.UNKNOWN, RootProvider.UNKNOWN, null)
            is RootResult.RootedNotGranted ->
                ResolvedRoot(RootStatus.NOT_GRANTED, result.provider, null)
        }
        _uiState.update {
            it.copy(
                rootStatus = resolved.status,
                rootProvider = resolved.provider,
                rootProviderVersion = resolved.version,
            )
        }
        Analytics.trackRootCheckResult(resolved.status.name)
        if (resolved.status == RootStatus.ROOTED) {
            Analytics.trackRootProvider(resolved.provider.name, resolved.version)
        }
    }

    fun onUpdateRequested() {
        appUpdateController.startFlexibleFlow()
    }

    fun onInstallRequested() {
        appUpdateController.completeUpdate()
    }

    override fun onCleared() {
        haptics.cancel()
    }
}
