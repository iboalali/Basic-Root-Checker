package com.iboalali.basicrootchecker

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.color.DynamicColors
import com.iboalali.basicrootchecker.navigation.AppNavigation
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        configureSplashScreenExitAnimation(splashScreen)

        DynamicColors.applyToActivityIfAvailable(this)
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        (application as BasicRootCheckerApplication).appUpdateController.attach(this)

        // Workaround: splash screen theme doesn't properly set light status bar
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_YES) == Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isNight

        setContent {
            BasicRootCheckerTheme {
                AppNavigation()
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
