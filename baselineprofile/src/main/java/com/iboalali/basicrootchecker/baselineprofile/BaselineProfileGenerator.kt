package com.iboalali.basicrootchecker.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the Baseline Profile shipped at `assets/dexopt/baseline.prof`.
 *
 * Run with `./gradlew :app:generateBaselineProfile` (all variants) or
 * `./gradlew :app:generateGplayReleaseBaselineProfile` for a single one. The output lands under
 * `app/src/<flavor>Release/generated/baselineProfiles/`.
 *
 * The journey deliberately covers cold startup AND scrolling/navigation so first-scroll and
 * secondary-screen entry are AOT-compiled, not left interpreted on first run.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = PACKAGE_NAME,
        // Also emit the captured paths as a startup profile (dexopt startup layout).
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        waitForMainScreen()

        // Main screen content (root status / device info / disclaimer cards).
        scrollList("main_list")

        // Each secondary screen, reached through the overflow menu, then scrolled.
        visitAndScroll("menu_settings", "settings_list")
        visitAndScroll("menu_about", "about_list")
        visitAndScroll("menu_licence", "licence_list")
    }
}
