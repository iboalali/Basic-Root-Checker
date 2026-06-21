package com.iboalali.basicrootchecker.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.ui.about.AboutScreen
import com.iboalali.basicrootchecker.ui.licence.LicenceScreen
import com.iboalali.basicrootchecker.ui.main.MainScreen
import com.iboalali.basicrootchecker.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object MainRoute : NavKey

@Serializable
data object AboutRoute : NavKey

@Serializable
data object LicenceRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

private val animation: ContentTransform = ContentTransform(
    targetContentEnter = slideInHorizontally(tween(300)) { it } +
            fadeIn(tween(300)),
    initialContentExit = slideOutHorizontally(tween(300)) { -it / 4 } +
            fadeOut(tween(300 / 2)),
)

/**
 * Pop transition that mirrors the forward push so the back animation follows the swipe side.
 *
 * @param towardRight the direction the dismissed (current) screen slides off-screen. The revealed
 * (previous) screen parallax-enters from that same side. Driven by the predictive-back swipe edge.
 */
private fun popTransition(towardRight: Boolean): ContentTransform = ContentTransform(
    targetContentEnter = slideInHorizontally(tween(300)) { if (towardRight) -it / 4 else it / 4 } +
            fadeIn(tween(300)),
    initialContentExit = slideOutHorizontally(tween(300)) { if (towardRight) it else -it } +
            fadeOut(tween(300 / 2)),
)


@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(MainRoute)

    // At the expanded width breakpoint (≥840dp: tablets, unfolded foldables in landscape, desktop
    // windows, XR panels) the secondary screens (Settings/About/Licence) open as a dialog over the
    // dimmed main screen instead of replacing it. Below it (phones, medium widths) they push
    // full-screen with the transitions defined above.
    val isExpanded = currentWindowAdaptiveInfoV2().windowSizeClass
        .isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
    val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }

    // Tag the secondary screens as dialogs only when expanded, so DialogSceneStrategy turns them
    // into a dismissable modal (scrim-tap / back / the close button all dismiss). With no metadata
    // they fall through to the single-pane push flow. entryProvider re-runs on width changes
    // (e.g. fold/unfold, window resize), so the presentation follows the current size live.
    val detailMetadata: Map<String, Any> =
        if (isExpanded) DialogSceneStrategy.dialog() else emptyMap()

    // The secondary screens show a back-arrow when pushed full-screen, and a close (X) when shown
    // as a dialog beside the still-visible main screen. The Dialog content inherits this local.
    val detailNavIcon = if (isExpanded) DetailNavIcon.CLOSE else DetailNavIcon.BACK

    // NavDisplay requires a non-empty back stack. Guard every pop so a double-back — a fast
    // system back-gesture, or tapping Up again while the exit animation still has the screen
    // composed — can never remove the root entry and crash with "backstack cannot be empty".
    val popBackStack: () -> Unit = {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    // Open a secondary screen, keeping the stack at [Main, oneDetail] (the three are interchangeable
    // siblings reached only from the main screen). On phones this is a no-op; it just guards against
    // ever stacking two secondary screens.
    val navigateToDetail: (NavKey) -> Unit = { route ->
        if (backStack.lastOrNull() != MainRoute) backStack.removeLastOrNull()
        backStack.add(route)
    }

    CompositionLocalProvider(LocalDetailNavIcon provides detailNavIcon) {
        NavDisplay(
            backStack = backStack,
            onBack = { popBackStack() },
            sceneStrategies = listOf(dialogStrategy),
            transitionSpec = { animation },
            popTransitionSpec = { popTransition(towardRight = true) },
            predictivePopTransitionSpec = { edge ->
                popTransition(towardRight = edge != NavigationEvent.EDGE_RIGHT)
            },
            entryProvider = entryProvider {
                entry<MainRoute> {
                    MainScreen(
                        onNavigateToAbout = {
                            Analytics.trackNavigation("/main", "/about")
                            navigateToDetail(AboutRoute)
                        },
                        onNavigateToLicence = {
                            Analytics.trackNavigation("/main", "/licence")
                            navigateToDetail(LicenceRoute)
                        },
                        onNavigateToSettings = {
                            Analytics.trackNavigation("/main", "/settings")
                            navigateToDetail(SettingsRoute)
                        },
                    )
                }

                entry<SettingsRoute>(metadata = detailMetadata) {
                    SettingsScreen(
                        onNavigateBack = {
                            Analytics.trackNavigation("/settings", "/main")
                            popBackStack()
                        },
                    )
                }

                entry<AboutRoute>(metadata = detailMetadata) {
                    AboutScreen(
                        onNavigateBack = {
                            Analytics.trackNavigation("/about", "/main")
                            popBackStack()
                        },
                    )
                }

                entry<LicenceRoute>(metadata = detailMetadata) {
                    LicenceScreen(
                        onNavigateBack = {
                            Analytics.trackNavigation("/licence", "/main")
                            popBackStack()
                        },
                    )
                }
            },
        )
    }
}
