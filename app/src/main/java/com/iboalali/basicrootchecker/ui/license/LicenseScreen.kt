package com.iboalali.basicrootchecker.ui.license

import android.text.util.Linkify
import android.widget.TextView
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.iboalali.basicrootchecker.R
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import com.iboalali.nav3.overlay.DetailNavigationIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseScreen(onNavigateBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

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
        val contentPadding =
            PaddingValues(
                top = innerPadding.calculateTopPadding(),
                start = innerPadding.calculateLeftPadding(layoutDirection),
                end = innerPadding.calculateRightPadding(layoutDirection),
            )
        val bottomPadding = innerPadding.calculateBottomPadding()

        Column(
            modifier =
                Modifier.testTag("license_list")
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(5.dp))

                Text(
                    text = stringResource(R.string.license_libsu),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.license_android_device_names),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(Modifier.height(20.dp))

                AndroidView(
                    factory = { ctx ->
                        TextView(ctx).apply {
                            text = ctx.getString(R.string.license_apache_title)
                            autoLinkMask = Linkify.WEB_URLS
                            setTextColor(textColor)
                            setLinkTextColor(linkColor)
                            textSize = 14f
                            gravity = android.view.Gravity.CENTER
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.license_apache_license),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(Modifier.height(50.dp + bottomPadding))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LicenseScreenPreview() {
    BasicRootCheckerTheme {
        LicenseScreen(onNavigateBack = {})
    }
}
