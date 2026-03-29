package com.iboalali.basicrootchecker.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
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

@Composable
fun AppNavigation() {
    val backStack = remember { mutableStateListOf<Any>(MainRoute) }

    val animDuration = 300

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        transitionSpec = {
            ContentTransform(
                targetContentEnter = slideInHorizontally(tween(animDuration)) { it } +
                    fadeIn(tween(animDuration)),
                initialContentExit = slideOutHorizontally(tween(animDuration)) { -it / 4 } +
                    fadeOut(tween(animDuration / 2)),
            )
        },
        popTransitionSpec = {
            ContentTransform(
                targetContentEnter = slideInHorizontally(tween(animDuration)) { -it / 4 } +
                    fadeIn(tween(animDuration)),
                initialContentExit = slideOutHorizontally(tween(animDuration)) { it } +
                    fadeOut(tween(animDuration / 2)),
            )
        },
        predictivePopTransitionSpec = {
            ContentTransform(
                targetContentEnter = slideInHorizontally(tween(animDuration)) { -it / 4 } +
                        fadeIn(tween(animDuration)),
                initialContentExit = slideOutHorizontally(tween(animDuration)) { it } +
                        fadeOut(tween(animDuration / 2)),
            )
        },
        entryProvider = entryProvider {
            entry<MainRoute> {
                MainScreen(
                    onNavigateToAbout = { backStack.add(AboutRoute) },
                    onNavigateToLicence = { backStack.add(LicenceRoute) },
                )
            }

            entry<AboutRoute> {
                AboutScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }

            entry<LicenceRoute> {
                LicenceScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
