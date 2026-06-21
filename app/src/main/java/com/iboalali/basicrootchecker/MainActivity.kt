package com.iboalali.basicrootchecker

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toDrawable
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.color.DynamicColors
import com.iboalali.basicrootchecker.data.ThemeMode
import com.iboalali.basicrootchecker.data.UserPreferences
import com.iboalali.basicrootchecker.ui.AppRoot
import com.iboalali.basicrootchecker.ui.LocalAppHaptics
import com.iboalali.basicrootchecker.ui.LocalHapticsEnabled
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        configureSplashScreenExitAnimation(splashScreen)

        DynamicColors.applyToActivityIfAvailable(this)
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        (application as BasicRootCheckerApplication).let { app ->
            app.appUpdateController.attach(this)
            app.billingController.attach(this)
            app.reviewController.attach(this)
            // Kick off the "Other apps" catalog refresh at launch; it updates in the background and
            // the About screen shows the cached/bundled list until (and unless) a fresher one arrives.
            app.appCatalogRepository.refresh()
        }

        val userPreferences = UserPreferences(this)
        // Read once synchronously so the first frame already uses the persisted theme, avoiding a
        // flash from the system default to the override. The splash screen masks this brief read.
        val initialThemeMode = runBlocking { userPreferences.themeMode.first() }

        val app = application as BasicRootCheckerApplication
        val billingController = app.billingController
        setContent {
            val themeMode by userPreferences.themeMode
                .collectAsStateWithLifecycle(initialValue = initialThemeMode)
            val hapticsEnabled by userPreferences.hapticsEnabled
                .collectAsStateWithLifecycle(initialValue = true)
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Keep status- and nav-bar icon contrast in sync with the resolved theme. Also works
            // around the splash screen theme not setting the light status bar on its own.
            LaunchedEffect(darkTheme) {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            BasicRootCheckerTheme(darkTheme = darkTheme) {
                // The window background comes from the XML theme, which follows the *system*
                // day/night setting and ignores the in-app override — so it would peek through
                // (in the wrong theme) during screen transitions. Drive it from the resolved
                // color scheme so the activity background matches, and cross-fades, with the theme.
                val backgroundColor = MaterialTheme.colorScheme.background
                SideEffect {
                    window.setBackgroundDrawable(backgroundColor.toArgb().toDrawable())
                }
                CompositionLocalProvider(
                    LocalHapticsEnabled provides hapticsEnabled,
                    LocalAppHaptics provides app.rootHaptics,
                ) {
                    AppRoot(tipCleared = billingController.tipCleared)
                }
            }
        }
    }

    private fun configureSplashScreenExitAnimation(splashScreen: SplashScreen) {
        splashScreen.setOnExitAnimationListener { splashScreenProvider ->
            Log.d("SplashScreen", "currentTime is ${System.currentTimeMillis()}")

            val startMillis = splashScreenProvider.iconAnimationStartMillis
            Log.d("SplashScreen", "startMillis is $startMillis")

            if (startMillis == 0L) {
                splashScreenProvider.remove()
                return@setOnExitAnimationListener
            }

            val timeDiff = startMillis - System.currentTimeMillis()
            Log.d("SplashScreen", "timeDiff is $timeDiff")
            Log.d("SplashScreen", "animation duration is ${splashScreenProvider.iconAnimationDurationMillis}")

            val exitTimeDelay = if (timeDiff <= 0) {
                splashScreenProvider.iconAnimationDurationMillis + timeDiff
            } else {
                splashScreenProvider.iconAnimationDurationMillis
            }

            Log.d("SplashScreen", "exitTimeDelay is $exitTimeDelay")

            ValueAnimator.ofFloat(1f, 0f).apply {
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationCancel(animation: Animator) {
                        Log.d("SplashScreen", "animation canceled")
                        splashScreenProvider.remove()
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        Log.d("SplashScreen", "animation ended")
                        splashScreenProvider.remove()
                    }
                })
                startDelay = exitTimeDelay
                addUpdateListener { animation ->
                    val animatedValue = animation.animatedValue as Float
                    splashScreenProvider.view.alpha = animatedValue
                    splashScreenProvider.iconView.alpha = animatedValue
                }
                start()
            }
        }
    }
}
