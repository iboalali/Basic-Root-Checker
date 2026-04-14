package com.iboalali.basicrootchecker.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.ui.about.AboutScreen
import com.iboalali.basicrootchecker.ui.licence.LicenceScreen
import com.iboalali.basicrootchecker.ui.main.MainScreen
import kotlinx.serialization.Serializable

@Serializable
data object MainRoute : NavKey

@Serializable
data object AboutRoute : NavKey

@Serializable
data object LicenceRoute : NavKey

private val animation: ContentTransform = ContentTransform(
    targetContentEnter = slideInHorizontally(tween(300)) { it } +
            fadeIn(tween(300)),
    initialContentExit = slideOutHorizontally(tween(300)) { -it / 4 } +
            fadeOut(tween(300 / 2)),
)


@Composable
fun AppNavigation() {
    val backStack = remember { mutableStateListOf<Any>(MainRoute) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        transitionSpec = { animation },
        popTransitionSpec = { animation },
        predictivePopTransitionSpec = { animation },
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
                )
            }

            entry<AboutRoute> {
                AboutScreen(
                    onNavigateBack = {
                        Analytics.trackNavigation("/about", "/main")
                        backStack.removeLastOrNull()
                    },
                )
            }

            entry<LicenceRoute> {
                LicenceScreen(
                    onNavigateBack = {
                        Analytics.trackNavigation("/licence", "/main")
                        backStack.removeLastOrNull()
                    },
                )
            }
        },
    )
}
