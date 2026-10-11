package com.iboalali.basicrootchecker.util

import androidx.compose.runtime.Composable
import com.iboalali.basicrootchecker.ui.about.AboutScreenContent
import com.iboalali.basicrootchecker.ui.license.LicenseScreen
import com.iboalali.basicrootchecker.ui.main.MainScreenContent
import com.iboalali.basicrootchecker.ui.main.MainUiState
import com.iboalali.basicrootchecker.ui.main.RootStatus
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import kotlinx.collections.immutable.persistentListOf

@PreviewPlayStoreListing
@Composable
private fun MainScreenPlayStoreListing() {
    BasicRootCheckerTheme {
        MainScreenContent(
            uiState = MainUiState(
                rootStatus = RootStatus.NOT_CHECKED,
                deviceMarketingName = "Pixel 8 Pro",
                deviceModelName = "husky",
                androidVersionName = "16",
            ),
            onCheckRoot = {},
            onRequestRoot = {},
            onUpdateRequested = {},
            onInstallRequested = {},
            onAppUpdatedSnackbarShown = {},
            onNavigateToAbout = {},
            onNavigateToLicense = {},
            onNavigateToSettings = {},
        )
    }
}

@PreviewPlayStoreListing
@Composable
private fun AboutScreenPlayStoreListing() {
    BasicRootCheckerTheme {
        AboutScreenContent(otherApps = persistentListOf(), onNavigateBack = {})
    }
}

@PreviewPlayStoreListing
@Composable
private fun LicenseScreenPlayStoreListing() {
    BasicRootCheckerTheme {
        LicenseScreen(onNavigateBack = {})
    }
}
