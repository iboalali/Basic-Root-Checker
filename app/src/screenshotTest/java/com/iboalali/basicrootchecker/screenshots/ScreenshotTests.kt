package com.iboalali.basicrootchecker.screenshots

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.iboalali.basicrootchecker.billing.TipProduct
import com.iboalali.basicrootchecker.billing.TipTier
import com.iboalali.basicrootchecker.data.RootProvider
import com.iboalali.basicrootchecker.data.ThemeMode
import com.iboalali.basicrootchecker.ui.about.AboutScreenContent
import com.iboalali.basicrootchecker.ui.about.OtherAppUi
import com.iboalali.basicrootchecker.ui.licence.LicenceScreen
import com.iboalali.basicrootchecker.ui.main.MainScreenContent
import com.iboalali.basicrootchecker.ui.main.MainUiState
import com.iboalali.basicrootchecker.ui.main.RootStatus
import com.iboalali.basicrootchecker.ui.settings.SettingsScreenContent
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import com.iboalali.basicrootchecker.util.PreviewPlayStoreNative
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.emptyFlow

/**
 * Play Store listing screenshots: one `@PreviewTest` per screen, each fanned out by
 * [PreviewPlayStoreNative] into 3 devices × 5 locales = 15 native-resolution PNGs.
 *
 * Generate / update them with `./gradlew :app:updateGplayDebugScreenshotTest`; the reference PNGs
 * land under `app/src/screenshotTestGplayDebug/reference/.../ScreenshotTestsKt/`, named
 * `<Function>_<Device>_<locale>_<hash>_0.png` (e.g. `MainRootedShot_Phone_ar_*.png`).
 * `:app:validateGplayDebugScreenshotTest` then guards them against unintended UI changes.
 *
 * The screens are rendered via their stateless `*Content` composables (reachable here because they
 * are `public`/`internal`) with fixed sample state, so the output is deterministic.
 */

@PreviewTest
@PreviewPlayStoreNative
@Composable
fun MainNotCheckedShot() {
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

@PreviewTest
@PreviewPlayStoreNative
@Composable
fun MainRootedShot() {
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

@PreviewTest
@PreviewPlayStoreNative
@Composable
fun SettingsShot() {
    BasicRootCheckerTheme {
        SettingsScreenContent(
            telemetryEnabled = true,
            onTelemetryEnabledChange = {},
            onResetIdentity = {},
            hapticsEnabled = true,
            onHapticsEnabledChange = {},
            themeMode = ThemeMode.SYSTEM,
            onThemeModeChange = {},
            currentLanguageTag = null,
            onLanguageSelected = {},
            tipJarAvailable = true,
            tipProducts = persistentListOf(
                TipProduct(TipTier.SMALL, "$1.99"),
                TipProduct(TipTier.MEDIUM, "$4.99"),
                TipProduct(TipTier.LARGE, "$9.99"),
            ),
            tipEvents = emptyFlow(),
            supporterTiers = persistentSetOf(TipTier.SMALL),
            onTipJarOpened = {},
            onTipSelected = {},
            onNavigateBack = {},
        )
    }
}

@PreviewTest
@PreviewPlayStoreNative
@Composable
fun AboutShot() {
    BasicRootCheckerTheme {
        AboutScreenContent(
            otherApps = persistentListOf(
                OtherAppUi(
                    name = "Billboard",
                    description = "Show large text on screen, as big as possible without cutting it off.",
                    iconUrl = null,
                    website = "https://iboalali.com/app/billboard/",
                    packageName = "com.iboalali.billboard",
                    whatsNew = persistentListOf("New **dark theme** and bigger text scaling"),
                ),
                OtherAppUi(
                    name = "Icon Recomposer",
                    description = "Light vector icons with a movable 3D emboss, then export to PNG, SVG, or VectorDrawable.",
                    iconUrl = null,
                    website = "https://iboalali.com/Icon-Recomposer/",
                    packageName = null,
                    whatsNew = persistentListOf("Your work is **saved automatically** and restored when you return"),
                ),
            ),
            onNavigateBack = {},
        )
    }
}

@PreviewTest
@PreviewPlayStoreNative
@Composable
fun LicenceShot() {
    BasicRootCheckerTheme {
        LicenceScreen(onNavigateBack = {})
    }
}
