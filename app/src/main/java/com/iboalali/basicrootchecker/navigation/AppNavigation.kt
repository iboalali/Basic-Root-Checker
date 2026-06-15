package com.iboalali.basicrootchecker.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
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


@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(MainRoute)

    // NavDisplay requires a non-empty back stack. Guard every pop so a double-back — a fast
    // system back-gesture, or tapping Up again while the exit animation still has the screen
    // composed — can never remove the root entry and crash with "backstack cannot be empty".
    val popBackStack: () -> Unit = {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    NavDisplay(
        backStack = backStack,
        onBack = { popBackStack() },
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
                        backStack.add(AboutRoute)
                    },
                    onNavigateToLicence = {
                        Analytics.trackNavigation("/main", "/licence")
                        backStack.add(LicenceRoute)
                    },
                    onNavigateToSettings = {
                        Analytics.trackNavigation("/main", "/settings")
                        backStack.add(SettingsRoute)
                    },
                )
            }

            entry<SettingsRoute> {
                SettingsScreen(
                    onNavigateBack = {
                        Analytics.trackNavigation("/settings", "/main")
                        popBackStack()
                    },
                )
            }

            entry<AboutRoute> {
                AboutScreen(
                    onNavigateBack = {
                        Analytics.trackNavigation("/about", "/main")
                        popBackStack()
                    },
                )
            }

            entry<LicenceRoute> {
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
