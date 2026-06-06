package com.iboalali.basicrootchecker.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iboalali.basicrootchecker.BasicRootCheckerApplication
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.billing.TipProduct
import com.iboalali.basicrootchecker.billing.TipPurchaseState
import com.iboalali.basicrootchecker.billing.TipTier
import com.iboalali.basicrootchecker.data.UserPreferences
import com.iboalali.basicrootchecker.util.AppLanguage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferences(application)

    private val billing = (application as BasicRootCheckerApplication).billingController

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

    val hapticsEnabled: StateFlow<Boolean> = prefs.hapticsEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true,
    )

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setHapticsEnabled(enabled)
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

    val tipProducts: StateFlow<List<TipProduct>> = billing.products

    val tipPurchaseState: StateFlow<TipPurchaseState> = billing.purchaseState

    /** Tiers whose durable record product is owned. Drives the debug view and future gating. */
    val supporterTiers: StateFlow<Set<TipTier>> = billing.supporterTiers

    fun onTipJarOpened() {
        Analytics.trackTipJarOpened()
    }

    fun onTipSelected(tier: TipTier) {
        Analytics.trackTipSelected(tier.name)
        billing.launchPurchase(tier)
    }

    fun onTipResultShown() {
        billing.consumeThanks()
    }
}
