package com.iboalali.basicrootchecker.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iboalali.basicrootchecker.BasicRootCheckerApplication
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.billing.TipProduct
import com.iboalali.basicrootchecker.billing.TipTier
import com.iboalali.basicrootchecker.data.ThemeMode
import com.iboalali.basicrootchecker.data.UserPreferences
import com.iboalali.basicrootchecker.util.AppLanguage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Analytics label for tips started from Settings (vs. the main screen's support card). */
private const val TIP_SOURCE_SETTINGS = "settings"

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferences(application)

    private val billing = (application as BasicRootCheckerApplication).billingController

    val telemetryEnabled: StateFlow<Boolean> =
        prefs.telemetryEnabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true,
        )

    fun setTelemetryEnabled(enabled: Boolean) {
        // viewModelScope is main-dispatched, and setTelemetryEnabled resumes back on it, so the
        // Analytics call lands on the main thread — which it must, since opting in can start the
        // SDK and TelemetryDeck.start registers a process lifecycle observer.
        viewModelScope.launch {
            prefs.setTelemetryEnabled(enabled)
            Analytics.setEnabled(getApplication(), enabled)
        }
    }

    /** Generates a fresh anonymous analytics identity, unlinking future data from the past. */
    fun resetTelemetryIdentity() {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            Analytics.resetIdentity(context)
        }
    }

    val hapticsEnabled: StateFlow<Boolean> =
        prefs.hapticsEnabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true,
        )

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setHapticsEnabled(enabled)
        }
    }

    val themeMode: StateFlow<ThemeMode> =
        prefs.themeMode.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeMode.SYSTEM,
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefs.setThemeMode(mode)
        }
    }

    /** Sets the app language. Pass `null` to follow the system default. */
    fun setLanguage(tag: String?) {
        AppLanguage.setLanguage(getApplication(), tag)
        Analytics.trackLanguageChanged(tag ?: "system")
    }

    // ---- Tip jar ----

    /** Whether the tip jar is supported in this build flavor (Google Play only). */
    val tipJarAvailable: Boolean = billing.isAvailable

    val tipProducts: StateFlow<ImmutableList<TipProduct>> = billing.products

    /** Tiers whose durable record product is owned. Drives the debug view and future gating. */
    val supporterTiers: StateFlow<ImmutableSet<TipTier>> = billing.supporterTiers

    fun onTipJarOpened() {
        Analytics.trackTipJarOpened(TIP_SOURCE_SETTINGS)
    }

    fun onTipSelected(tier: TipTier) {
        Analytics.trackTipSelected(tier.name)
        billing.launchPurchase(tier)
    }
}
