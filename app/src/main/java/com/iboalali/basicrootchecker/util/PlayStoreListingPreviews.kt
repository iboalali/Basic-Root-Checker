package com.iboalali.basicrootchecker.util

import androidx.compose.runtime.Composable
import com.iboalali.basicrootchecker.ui.about.AboutScreen
import com.iboalali.basicrootchecker.ui.licence.LicenceScreen
import com.iboalali.basicrootchecker.ui.main.MainScreenContent
import com.iboalali.basicrootchecker.ui.main.MainUiState
import com.iboalali.basicrootchecker.ui.main.RootStatus
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme

@PreviewPlayStoreListing
@Composable
private fun MainScreenPlayStoreListing() {
    BasicRootCheckerTheme {
        MainScreenContent(
            uiState = MainUiState(
                rootStatus = RootStatus.NOT_CHECKED,
                deviceMarketingName = "Pixel 8 Pro",
                deviceModelName = "husky",
                androidVersion = "Android 16",
            ),
            onCheckRoot = {},
            onNavigateToAbout = {},
            onNavigateToLicence = {},
        )
    }
}

@PreviewPlayStoreListing
@Composable
private fun AboutScreenPlayStoreListing() {
    BasicRootCheckerTheme {
        AboutScreen(onNavigateBack = {})
    }
}

@PreviewPlayStoreListing
@Composable
private fun LicenceScreenPlayStoreListing() {
    BasicRootCheckerTheme {
        LicenceScreen(onNavigateBack = {})
    }
}
