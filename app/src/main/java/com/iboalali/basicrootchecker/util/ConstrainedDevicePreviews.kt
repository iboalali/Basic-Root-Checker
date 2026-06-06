package com.iboalali.basicrootchecker.util

import androidx.compose.runtime.Composable
import com.iboalali.basicrootchecker.billing.TipPurchaseState
import com.iboalali.basicrootchecker.data.RootProvider
import com.iboalali.basicrootchecker.ui.about.AboutScreen
import com.iboalali.basicrootchecker.ui.licence.LicenceScreen
import com.iboalali.basicrootchecker.ui.main.MainScreenContent
import com.iboalali.basicrootchecker.ui.main.MainUiState
import com.iboalali.basicrootchecker.ui.main.RootStatus
import com.iboalali.basicrootchecker.ui.settings.SettingsScreenContent
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme

@PreviewConstrainedDevices
@Composable
private fun MainScreenConstrainedPreview() {
    BasicRootCheckerTheme {
        MainScreenContent(
            uiState = MainUiState(
                rootStatus = RootStatus.NOT_CHECKED,
                deviceMarketingName = "Pixel 8 Pro",
                deviceModelName = "husky",
                androidVersion = "Android 16",
            ),
            onCheckRoot = {},
            onRequestRoot = {},
            onUpdateRequested = {},
            onInstallRequested = {},
            onAppUpdatedSnackbarShown = {},
            onNavigateToAbout = {},
            onNavigateToLicence = {},
            onNavigateToSettings = {},
        )
    }
}

@PreviewConstrainedDevices
@Composable
private fun MainScreenRootedConstrainedPreview() {
    BasicRootCheckerTheme {
        MainScreenContent(
            uiState = MainUiState(
                rootStatus = RootStatus.ROOTED,
                rootProvider = RootProvider.MAGISK,
                rootProviderVersion = "27.0",
                deviceMarketingName = "Pixel 8 Pro",
                deviceModelName = "husky",
                androidVersion = "Android 16",
            ),
            onCheckRoot = {},
            onRequestRoot = {},
            onUpdateRequested = {},
            onInstallRequested = {},
            onAppUpdatedSnackbarShown = {},
            onNavigateToAbout = {},
            onNavigateToLicence = {},
            onNavigateToSettings = {},
        )
    }
}

@PreviewConstrainedDevices
@Composable
private fun MainScreenNotGrantedConstrainedPreview() {
    BasicRootCheckerTheme {
        MainScreenContent(
            uiState = MainUiState(
                rootStatus = RootStatus.NOT_GRANTED,
                rootProvider = RootProvider.MAGISK,
                deviceMarketingName = "Pixel 8 Pro",
                deviceModelName = "husky",
                androidVersion = "Android 16",
            ),
            onCheckRoot = {},
            onRequestRoot = {},
            onUpdateRequested = {},
            onInstallRequested = {},
            onAppUpdatedSnackbarShown = {},
            onNavigateToAbout = {},
            onNavigateToLicence = {},
            onNavigateToSettings = {},
        )
    }
}

@PreviewConstrainedDevices
@Composable
private fun AboutScreenConstrainedPreview() {
    BasicRootCheckerTheme {
        AboutScreen(onNavigateBack = {})
    }
}

@PreviewConstrainedDevices
@Composable
private fun LicenceScreenConstrainedPreview() {
    BasicRootCheckerTheme {
        LicenceScreen(onNavigateBack = {})
    }
}

@PreviewConstrainedDevices
@Composable
private fun SettingsScreenConstrainedPreview() {
    BasicRootCheckerTheme {
        SettingsScreenContent(
            telemetryEnabled = true,
            onTelemetryEnabledChange = {},
            hapticsEnabled = true,
            onHapticsEnabledChange = {},
            currentLanguageTag = "en",
            onLanguageSelected = {},
            tipJarAvailable = true,
            tipProducts = emptyList(),
            tipPurchaseState = TipPurchaseState.Idle,
            onTipJarOpened = {},
            onTipSelected = {},
            onTipResultShown = {},
            onNavigateBack = {},
        )
    }
}
