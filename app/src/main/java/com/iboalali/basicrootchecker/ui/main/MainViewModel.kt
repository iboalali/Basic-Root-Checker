package com.iboalali.basicrootchecker.ui.main

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iboalali.basicrootchecker.R
import com.iboalali.basicrootchecker.data.RootChecker
import com.iboalali.basicrootchecker.util.DeviceInfo
import de.boehrsi.devicemarketingnames.DeviceMarketingNames
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadDeviceInfo()
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
            val result = RootChecker.checkRoot()
            val status = when (result) {
                true -> RootStatus.ROOTED
                false -> RootStatus.NOT_ROOTED
                null -> RootStatus.UNKNOWN
            }
            _uiState.update { it.copy(rootStatus = status) }
        }
    }
}
