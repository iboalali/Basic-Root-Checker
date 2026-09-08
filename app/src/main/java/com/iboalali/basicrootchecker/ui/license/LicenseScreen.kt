package com.iboalali.basicrootchecker.ui.license

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.iboalali.basicrootchecker.R
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import com.iboalali.nav3.overlay.DetailNavigationIcon
import com.iboalali.ui.licences.AndroidSharedAttributions
import com.iboalali.ui.licences.OssCredits
import com.iboalali.ui.licences.OssCreditsMaxWidth
import com.iboalali.ui.licences.OssLibrary
import com.iboalali.ui.licences.OssLicenses

/**
 * The third-party libraries **this app** pulls in directly.
 *
 * Everything arriving through the shared modules — Coil, OkHttp, kotlinx.coroutines,
 * kotlinx.serialization, the TelemetryDeck SDK — is in `AndroidSharedAttributions` and must not be
 * repeated here. Four of them were credited *nowhere* in this app before that list existed, and all
 * four are Apache 2.0, whose §4(a) obliges us to pass the license on.
 */
private val BasicRootCheckerLibraries =
    listOf(
        OssLibrary(
            name = "libsu",
            author = "topjohnwu",
            url = "https://github.com/topjohnwu/libsu",
            license = OssLicenses.Apache2_0,
        ),
        OssLibrary(
            // This app previously credited "AndroidDeviceNames" by Jared Rummler. That is a
            // *different project* — the dependency is and was `de.boehrsi:devicemarketingnames`,
            // which is Boehrsi's, so the old credit named the wrong library and the wrong author.
            //
            // Apache 2.0 verified upstream 2026-08-17: the published artifact carries no license
            // metadata at all (no LICENSE in the AAR, no `<licenses>` in the POM, no source
            // header),
            // but the repository's `LICENSE.txt` is the Apache 2.0 text byte-identical to the copy
            // `:ui:licences` ships, and there is no NOTICE file, so §4(d) adds nothing.
            name = "DeviceMarketingNames",
            author = "Boehrsi",
            url = "https://github.com/Boehrsi/DeviceMarketingNames",
            license = OssLicenses.Apache2_0,
        ),
        OssLibrary(
            name = "kotlinx.collections.immutable",
            author = "Kotlin",
            url = "https://github.com/Kotlin/kotlinx.collections.immutable",
            license = OssLicenses.Apache2_0,
        ),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseScreen(onNavigateBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.action_license)) },
                // Back-arrow when pushed full-screen; a close (X) when shown as a dialog over the
                // main screen on large screens (see LocalDetailNavIcon). Glyphs and content
                // descriptions come from LocalDetailOverlayStyle, and the haptic tap is the shared
                // component's — onNavigateBack goes in unwrapped.
                navigationIcon = { DetailNavigationIcon(onBack = onNavigateBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        // Bottom inset is deliberately excluded here and added as a trailing spacer instead, so the
        // list scrolls *under* the navigation bar rather than stopping short of it.
        val contentPadding =
            PaddingValues(
                top = innerPadding.calculateTopPadding(),
                start = innerPadding.calculateLeftPadding(layoutDirection),
                end = innerPadding.calculateRightPadding(layoutDirection),
            )
        val bottomPadding = innerPadding.calculateBottomPadding()

        Column(
            // `license_list` is load-bearing: the Baseline Profile journey waits on it and
            // `StartupBenchmarks` flings it. It has to stay on the scrollable node, which is why
            // `OssCredits` ships content rather than its own scroll container.
            modifier =
                Modifier.testTag("license_list")
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OssCredits(
                libraries = BasicRootCheckerLibraries + AndroidSharedAttributions,
                modifier = Modifier.widthIn(max = OssCreditsMaxWidth).fillMaxWidth(),
            )
            Spacer(Modifier.height(bottomPadding))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LicenseScreenPreview() {
    BasicRootCheckerTheme { LicenseScreen(onNavigateBack = {}) }
}
