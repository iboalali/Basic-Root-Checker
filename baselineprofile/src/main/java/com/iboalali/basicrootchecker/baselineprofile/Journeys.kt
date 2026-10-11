package com.iboalali.basicrootchecker.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/**
 * Shared UI Automator journeys for the Baseline Profile generator and the Macrobenchmarks.
 *
 * Elements are located by [By.res] against the Compose `Modifier.testTag`s, which are surfaced as
 * resource-ids by `testTagsAsResourceId = true` in `AppRoot`. This keeps the journeys independent
 * of the active locale.
 */

/** Release applicationId, the variant the profile ships in (release has no id suffix). */
const val PACKAGE_NAME = "com.iboalali.basicrootchecker"

private const val TIMEOUT = 5_000L

/**
 * Every wait in these journeys is required. A missing element must fail the run, or a renamed
 * testTag silently drops whole screens from the generated profile while the generator passes.
 */
private fun MacrobenchmarkScope.requireObject(tag: String): UiObject2 =
    checkNotNull(device.wait(Until.findObject(By.res(tag)), TIMEOUT)) {
        "No element with testTag \"$tag\" within $TIMEOUT ms"
    }

private fun MacrobenchmarkScope.awaitObject(tag: String) {
    check(device.wait(Until.hasObject(By.res(tag)), TIMEOUT)) {
        "No element with testTag \"$tag\" within $TIMEOUT ms"
    }
}

fun MacrobenchmarkScope.waitForMainScreen() {
    awaitObject("main_list")
}

/**
 * Fling a `verticalScroll` list (located by [tag]) down and back up. The flings are harmless if
 * the content is shorter than the viewport. The gesture margin keeps flings clear of the system
 * gesture area on gesture-nav devices.
 */
fun MacrobenchmarkScope.scrollList(tag: String) {
    val list = requireObject(tag)
    list.setGestureMargin(device.displayWidth / 5)
    list.fling(Direction.DOWN)
    device.waitForIdle()
    list.fling(Direction.UP)
    device.waitForIdle()
}

/** Open the main screen's overflow menu and tap the item with [menuItemTag]. */
fun MacrobenchmarkScope.openMenuItem(menuItemTag: String) {
    requireObject("overflow_menu").click()
    requireObject(menuItemTag).click()
}

/**
 * Open a secondary screen via the overflow [menuItemTag], scroll its [listTag] content, then
 * return to the main screen.
 */
fun MacrobenchmarkScope.visitAndScroll(menuItemTag: String, listTag: String) {
    openMenuItem(menuItemTag)
    awaitObject(listTag)
    scrollList(listTag)
    device.pressBack()
    waitForMainScreen()
}

/**
 * Launch the app and navigate to the License screen (the longest scrollable content), leaving it
 * on screen and ready to scroll. Intended for a FrameTiming benchmark's `setupBlock` after a
 * `killProcess()`, so the measured block only times the scroll, not the launch or navigation.
 */
fun MacrobenchmarkScope.openLicenseScreen() {
    startActivityAndWait()
    waitForMainScreen()
    openMenuItem("menu_license")
    awaitObject("license_list")
}
