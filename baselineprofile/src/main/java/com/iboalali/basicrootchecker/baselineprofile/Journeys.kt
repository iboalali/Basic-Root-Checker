package com.iboalali.basicrootchecker.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

/**
 * Shared UI Automator journeys for the Baseline Profile generator and the Macrobenchmarks.
 *
 * Elements are located by [By.res] against the Compose `Modifier.testTag`s, which are surfaced as
 * resource-ids by `testTagsAsResourceId = true` in `AppRoot`. This keeps the journeys independent
 * of the active locale (the app ships en/de/ar/es/ru).
 */

/** Release applicationId — the variant the profile ships in (release has no id suffix). */
const val PACKAGE_NAME = "com.iboalali.basicrootchecker"

private const val TIMEOUT = 5_000L

/** Wait until the main screen's content list is present. */
fun MacrobenchmarkScope.waitForMainScreen() {
    device.wait(Until.hasObject(By.res("main_list")), TIMEOUT)
}

/**
 * Fling a `verticalScroll` list (located by [tag]) down and back up. No-ops gracefully if the
 * content is shorter than the viewport. The gesture margin keeps flings clear of the system
 * gesture area on gesture-nav devices.
 */
fun MacrobenchmarkScope.scrollList(tag: String) {
    val list = device.wait(Until.findObject(By.res(tag)), TIMEOUT) ?: return
    list.setGestureMargin(device.displayWidth / 5)
    list.fling(Direction.DOWN)
    device.waitForIdle()
    list.fling(Direction.UP)
    device.waitForIdle()
}

/** Open the main screen's overflow menu and tap the item with [menuItemTag]. */
fun MacrobenchmarkScope.openMenuItem(menuItemTag: String) {
    device.wait(Until.findObject(By.res("overflow_menu")), TIMEOUT)?.click()
    device.wait(Until.findObject(By.res(menuItemTag)), TIMEOUT)?.click()
}

/**
 * Open a secondary screen via the overflow [menuItemTag], scroll its [listTag] content, then
 * return to the main screen.
 */
fun MacrobenchmarkScope.visitAndScroll(menuItemTag: String, listTag: String) {
    openMenuItem(menuItemTag)
    device.wait(Until.hasObject(By.res(listTag)), TIMEOUT)
    scrollList(listTag)
    device.pressBack()
    waitForMainScreen()
}

/**
 * Launch the app and navigate to the Licence screen (the longest scrollable content), leaving it
 * on screen and ready to scroll. Intended for a FrameTiming benchmark's `setupBlock` after a
 * `killProcess()`, so the measured block only times the scroll, not the launch or navigation.
 */
fun MacrobenchmarkScope.openLicenceScreen() {
    startActivityAndWait()
    waitForMainScreen()
    openMenuItem("menu_licence")
    device.wait(Until.hasObject(By.res("licence_list")), TIMEOUT)
}
