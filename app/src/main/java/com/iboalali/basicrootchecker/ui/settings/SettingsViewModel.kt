package com.iboalali.basicrootchecker.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.data.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferences(application)

    val telemetryEnabled: StateFlow<Boolean> = prefs.telemetryEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true,
    )

    fun setTelemetryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setTelemetryEnabled(enabled)
            Analytics.setEnabled(enabled)
        }
    }
}
