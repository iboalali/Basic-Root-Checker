package com.iboalali.basicrootchecker.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.iboalali.basicrootchecker.billing.TipProduct
import com.iboalali.basicrootchecker.billing.TipTier
import com.iboalali.basicrootchecker.data.RootProvider
import com.iboalali.basicrootchecker.data.ThemeMode
import com.iboalali.basicrootchecker.navigation.DetailNavIcon
import com.iboalali.basicrootchecker.navigation.LocalDetailNavIcon
import com.iboalali.basicrootchecker.ui.about.AboutScreenContent
import com.iboalali.basicrootchecker.ui.about.OtherAppUi
import com.iboalali.basicrootchecker.ui.licence.LicenceScreen
import com.iboalali.basicrootchecker.ui.main.MainScreenContent
import com.iboalali.basicrootchecker.ui.main.MainUiState
import com.iboalali.basicrootchecker.ui.main.RootStatus
import com.iboalali.basicrootchecker.ui.settings.SettingsScreenContent
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import com.iboalali.basicrootchecker.util.PreviewPlayStorePhone
import com.iboalali.basicrootchecker.util.PreviewPlayStoreTablet7
import com.iboalali.basicrootchecker.util.PreviewPlayStoreTablet10
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.emptyFlow

/**
 * Play Store listing screenshots, rendered on the JVM via Layoutlib (no device). The matrices are
 * split per Play Console slot because the app's navigation is adaptive at the 840dp width breakpoint
 * (see `AppNavigation`):
 * - **Phone + 7-inch** ([PreviewPlayStorePhone] + [PreviewPlayStoreTablet7], both portrait < 840dp):
 *   each secondary screen renders **single-pane / full-screen**, one `@PreviewTest` per screen with
 *   both annotations so the same shot renders at phone and 7-inch sizes.
 * - **10-inch** ([PreviewPlayStoreTablet10], landscape 1280dp ≥ 840dp): the secondary screens render
 *   as a **dialog card over the dimmed main screen** (`*DialogShot`), mirroring the
 *   `DialogSceneStrategy` presentation in `AppNavigation`. The main screen stays single-pane at every
 *   width, so its shots ([MainRootedShot] / [MainNotCheckedShot]) use all three matrices.
 *
 * Generate / update with `./gradlew :app:updateGplayDebugScreenshotTest`; references land under
 * `app/src/screenshotTestGplayDebug/reference/.../ScreenshotTestsKt/` as
 * `<Function>_<Device>_<locale>_<hash>_0.png`. `:app:validateGplayDebugScreenshotTest` then guards
 * them. The screens render via their stateless `*Content` composables (reachable here because they
 * are `public`/`internal`) with fixed sample state, so output is deterministic.
 */

// ---- Reusable screen content (shared by the single-pane shots and the tablet dialogs) -----------

@Composable
private fun MainNotChecked() {
    MainScreenContent(
        uiState =
            MainUiState(
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

@Composable
private fun MainRooted() {
    MainScreenContent(
        uiState =
            MainUiState(
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

@Composable
private fun Settings() {
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
        tipProducts =
            persistentListOf(
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

@Composable
private fun About() {
    AboutScreenContent(
        otherApps =
            persistentListOf(
                OtherAppUi(
                    name = "Billboard",
                    description = "Show large text on screen, as big as possible without cutting it off.",
                    iconUrl = null,
                    website = "https://iboalali.com/app/billboard/",
                    packageName = "com.iboalali.billboard",
                    highlights = persistentListOf("New **dark theme** and bigger text scaling"),
                ),
                OtherAppUi(
                    name = "Icon Recomposer",
                    description = "Light vector icons with a movable 3D emboss, then export to PNG, SVG, or VectorDrawable.",
                    iconUrl = null,
                    website = "https://iboalali.com/Icon-Recomposer/",
                    packageName = null,
                    highlights = persistentListOf("Your work is **saved automatically** and restored when you return"),
                ),
            ),
        onNavigateBack = {},
    )
}

@Composable
private fun Licence() {
    LicenceScreen(onNavigateBack = {})
}

/**
 * A secondary screen as the app shows it on a large screen (≥840dp): a centered rounded card over the
 * dimmed main screen. Mirrors the `DialogSceneStrategy` presentation in `AppNavigation` — the screen
 * renders under `LocalDetailNavIcon = CLOSE`, so it draws the close (✕) icon and rounds its own
 * corners via `detailDialogShape()`, exactly as it would inside the real dialog.
 */
@Composable
private fun DialogOverMain(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        MainRooted()
        Box(
            modifier =
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)).padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalDetailNavIcon provides DetailNavIcon.CLOSE) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    tonalElevation = 6.dp,
                    modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().fillMaxHeight(0.9f),
                ) {
                    content()
                }
            }
        }
    }
}

// ---- Main screen — single-pane at every width, so it spans all three matrices --------------------

@PreviewTest
@PreviewPlayStorePhone
@PreviewPlayStoreTablet7
@PreviewPlayStoreTablet10
@Composable
fun MainNotCheckedShot() {
    BasicRootCheckerTheme { MainNotChecked() }
}

@PreviewTest
@PreviewPlayStorePhone
@PreviewPlayStoreTablet7
@PreviewPlayStoreTablet10
@Composable
fun MainRootedShot() {
    BasicRootCheckerTheme { MainRooted() }
}

// ---- Secondary screens, single-pane (phone + 7-inch, < 840dp) ------------------------------------

@PreviewTest
@PreviewPlayStorePhone
@PreviewPlayStoreTablet7
@Composable
fun SettingsShot() {
    BasicRootCheckerTheme { Settings() }
}

@PreviewTest
@PreviewPlayStorePhone
@PreviewPlayStoreTablet7
@Composable
fun AboutShot() {
    BasicRootCheckerTheme { About() }
}

@PreviewTest
@PreviewPlayStorePhone
@PreviewPlayStoreTablet7
@Composable
fun LicenceShot() {
    BasicRootCheckerTheme { Licence() }
}

// ---- Secondary screens on the 10-inch tablet (≥840dp) — dialog over the dimmed main screen --------

@PreviewTest
@PreviewPlayStoreTablet10
@Composable
fun SettingsDialogShot() {
    BasicRootCheckerTheme { DialogOverMain { Settings() } }
}

@PreviewTest
@PreviewPlayStoreTablet10
@Composable
fun AboutDialogShot() {
    BasicRootCheckerTheme { DialogOverMain { About() } }
}

@PreviewTest
@PreviewPlayStoreTablet10
@Composable
fun LicenceDialogShot() {
    BasicRootCheckerTheme { DialogOverMain { Licence() } }
}
