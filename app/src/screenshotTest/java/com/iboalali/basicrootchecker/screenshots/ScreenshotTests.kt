package com.iboalali.basicrootchecker.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.tools.screenshot.PreviewTest
import com.iboalali.appcatalog.ui.OtherApp
import com.iboalali.basicrootchecker.billing.TipProduct
import com.iboalali.basicrootchecker.billing.TipTier
import com.iboalali.basicrootchecker.data.RootProvider
import com.iboalali.ui.theme.ThemeMode
import com.iboalali.basicrootchecker.ui.about.AboutScreenContent
import com.iboalali.basicrootchecker.ui.license.LicenseScreen
import com.iboalali.basicrootchecker.ui.main.MainScreenContent
import com.iboalali.basicrootchecker.ui.main.MainUiState
import com.iboalali.basicrootchecker.ui.main.RootStatus
import com.iboalali.basicrootchecker.ui.settings.SettingsScreenContent
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import com.iboalali.basicrootchecker.update.AppUpdateEvent
import com.iboalali.nav3.overlay.DetailCard
import com.iboalali.previews.matrix.PreviewPlayStorePhone
import com.iboalali.previews.matrix.PreviewPlayStoreTablet10
import com.iboalali.previews.matrix.PreviewPlayStoreTablet7
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * Compose screenshots, rendered on the JVM via Layoutlib (no device). Two kinds live here:
 * - **Listing shots**, which double as the Play Store upload — everything below except the update
 *   flow. The matrices are split per Play Console slot, as described next.
 * - **Regression-only shots** for states the store should never show, currently the in-app update
 *   card. `render.excludeShots` in "Play Store/store.json" keeps these out of the export.
 *
 * The matrices are split per Play Console slot because the app's navigation is adaptive at the
 * 840dp width breakpoint (see `AppNavigation`):
 * - **Phone + 7-inch** ([PreviewPlayStorePhone] + [PreviewPlayStoreTablet7], both portrait <
 *   840dp): each secondary screen renders **single-pane / full-screen**, one `@PreviewTest` per
 *   screen with both annotations so the same shot renders at phone and 7-inch sizes.
 * - **10-inch** ([PreviewPlayStoreTablet10], landscape 1280dp ≥ 840dp): the secondary screens
 *   render as a **dialog card over the dimmed main screen** (`*DialogShot`) via the same
 *   [DetailCard] the custom `DetailOverlayScene` uses at expanded width — at its resting open
 *   state, since the overlay's container transform is motion the renderer can't advance. The main
 *   screen stays single-pane at every width, so its shots ([MainRootedShot] / [MainNotCheckedShot])
 *   use all three matrices.
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
        onNavigateToLicense = {},
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
        onNavigateToLicense = {},
        onNavigateToSettings = {},
    )
}

/**
 * The main screen with the in-app update card in [updateStatus]. Built on the not-checked state,
 * which is where an update card is actually met: the update check runs at launch, before anyone has
 * tapped the FAB. That also keeps these shots about the card — the root-result rendering is already
 * covered by [MainRootedShot].
 */
@Composable
private fun MainWithUpdate(updateStatus: AppUpdateEvent) {
    MainScreenContent(
        uiState =
            MainUiState(
                rootStatus = RootStatus.NOT_CHECKED,
                deviceMarketingName = "Pixel 8 Pro",
                deviceModelName = "husky",
                androidVersion = "Android 16",
                updateStatus = updateStatus,
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
                OtherApp(
                    name = "Billboard",
                    description =
                        "Show large text on screen, as big as possible without cutting it off.",
                    iconUrl = null,
                    website = "https://iboalali.com/app/billboard/",
                    packageName = "com.iboalali.billboard",
                    highlights = persistentListOf("New **dark theme** and bigger text scaling"),
                ),
                OtherApp(
                    name = "Icon Recomposer",
                    description =
                        "Light vector icons with a movable 3D emboss, then export to PNG, SVG, or VectorDrawable.",
                    iconUrl = null,
                    website = "https://iboalali.com/Icon-Recomposer/",
                    packageName = null,
                    highlights =
                        persistentListOf(
                            "Your work is **saved automatically** and restored when you return"
                        ),
                ),
            ),
        onNavigateBack = {},
    )
}

@Composable
private fun License() {
    LicenseScreen(onNavigateBack = {})
}

/**
 * A secondary screen as the app shows it on a large screen (≥840dp): a centered rounded card over
 * the dimmed main screen. Renders the same [DetailCard] the live `DetailOverlayScene` uses (so this
 * regression baseline stays representative), at its resting open state — full scrim, no drag
 * offset. [DetailCard] provides `LocalDetailNavIcon = CLOSE`, so the screen draws the close (✕)
 * icon.
 */
@Composable
private fun DialogOverMain(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        MainRooted()
        DetailCard(cardModifier = Modifier.fillMaxHeight(0.9f), content = content)
    }
}

// ---- Main screen — single-pane at every width, so it spans all three matrices
// --------------------

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

// ---- In-app update flow — regression only, never uploaded
// ----------------------------------------

/*
 * Every state the update card can be in, which is otherwise reachable only through the debug
 * "Demo: in-app update" overflow item.
 *
 * Two matrices, because the card is `widthIn(max = 600.dp)` and width is the only axis it responds
 * to. [PreviewPlayStorePhone] (411dp) is below the cap, so the card fills the width and the long
 * German and Russian strings are at their tightest. [PreviewPlayStoreTablet10] is above it, and its
 * 800dp landscape height is the least vertical room the column ever gets. Tablet7 is capped-width
 * and tall, so it adds nothing either of those two doesn't already show.
 *
 * These are a regression baseline, not store copy — an update prompt does not sell the app. They
 * are listed in `render.excludeShots` in "Play Store/store.json", which keeps them out of the
 * export that feeds the Play Console.
 */

@PreviewTest
@PreviewPlayStorePhone
@PreviewPlayStoreTablet10
@Composable
fun UpdateAvailableShot() {
    BasicRootCheckerTheme { MainWithUpdate(AppUpdateEvent.Available) }
}

/**
 * Mid-download. `UpdateCard` drives the bar through `animateFloatAsState`, which initializes at its
 * target rather than animating up from zero, so the renderer lands on a stable 3.5 / 12.0 MB.
 */
@PreviewTest
@PreviewPlayStorePhone
@PreviewPlayStoreTablet10
@Composable
fun UpdateDownloadingShot() {
    BasicRootCheckerTheme {
        MainWithUpdate(
            AppUpdateEvent.Downloading(bytesDownloaded = 3_500_000, totalBytes = 12_000_000)
        )
    }
}

@PreviewTest
@PreviewPlayStorePhone
@PreviewPlayStoreTablet10
@Composable
fun UpdateDownloadedShot() {
    BasicRootCheckerTheme { MainWithUpdate(AppUpdateEvent.Downloaded) }
}

/** The error code is not rendered — the card shows one generic line — so any value will do. */
@PreviewTest
@PreviewPlayStorePhone
@PreviewPlayStoreTablet10
@Composable
fun UpdateFailedShot() {
    BasicRootCheckerTheme { MainWithUpdate(AppUpdateEvent.Failed(errorCode = -100)) }
}

// ---- Secondary screens, single-pane (phone + 7-inch, < 840dp)
// ------------------------------------

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
fun LicenseShot() {
    BasicRootCheckerTheme { License() }
}

// ---- Secondary screens on the 10-inch tablet (≥840dp) — dialog over the dimmed main screen
// --------

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
fun LicenseDialogShot() {
    BasicRootCheckerTheme { DialogOverMain { License() } }
}
