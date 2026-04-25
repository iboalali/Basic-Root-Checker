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
import com.iboalali.basicrootchecker.data.UserPreferences
import com.iboalali.basicrootchecker.update.AppUpdateEvent
import com.iboalali.basicrootchecker.util.DeviceInfo
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
}

data class MainUiState(
    val rootStatus: RootStatus = RootStatus.NOT_CHECKED,
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
            _uiState.update { it.copy(rootStatus = RootStatus.CHECKING) }
            Analytics.trackRootCheckStarted()
            val result = RootChecker.checkRoot()
            val status = when (result) {
                true -> RootStatus.ROOTED
                false -> RootStatus.NOT_ROOTED
                null -> RootStatus.UNKNOWN
            }
            _uiState.update { it.copy(rootStatus = status) }
            Analytics.trackRootCheckResult(status.name)
        }
    }

    fun onUpdateRequested() {
        appUpdateController.startFlexibleFlow()
    }

    fun onInstallRequested() {
        appUpdateController.completeUpdate()
    }
}
