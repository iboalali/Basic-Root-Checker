package com.iboalali.basicrootchecker.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves the Baseline Profile moved the numbers.
 *
 * Each metric has an A/B pair: a `*NoCompilation` test pinned to [CompilationMode.None] and a
 * `*BaselineProfile` test using `CompilationMode.Partial(BaselineProfileMode.Require)` — `Require`
 * fails loudly if the profile is missing rather than silently measuring an unprofiled build.
 * Compare the **medians** the two report.
 *
 * Run on the connected device with `./gradlew :baselineprofile:connectedGplayBenchmarkAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmarks {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupNoCompilation() = startup(CompilationMode.None())

    @Test
    fun startupBaselineProfile() =
        startup(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun startup(compilationMode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
        compilationMode = compilationMode,
    ) {
        pressHome()
        startActivityAndWait()
    }

    @Test
    fun scrollNoCompilation() = scroll(CompilationMode.None())

    @Test
    fun scrollBaselineProfile() =
        scroll(CompilationMode.Partial(BaselineProfileMode.Require))

    /**
     * Scroll the Licence screen — the longest scrollable content in the app — so FrameTimingMetric
     * has real frames to measure.
     *
     * No [StartupMode] here: with a startup mode set, the process is killed *after* `setupBlock`, so
     * the navigation done there would be lost and the measured scroll would find an empty screen
     * ("no renderthread slices"). Instead `setupBlock` kills the process itself and re-launches +
     * navigates each iteration, leaving only the scroll to be timed in the measured block.
     */
    private fun scroll(compilationMode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        iterations = 7,
        compilationMode = compilationMode,
        setupBlock = {
            killProcess()
            openLicenceScreen()
        },
    ) {
        scrollList("licence_list")
    }
}
