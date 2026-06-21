package com.iboalali.basicrootchecker.ui.about

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iboalali.basicrootchecker.BasicRootCheckerApplication
import com.iboalali.basicrootchecker.R
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.navigation.DetailNavIcon
import com.iboalali.basicrootchecker.navigation.LocalDetailNavIcon
import com.iboalali.basicrootchecker.navigation.detailDialogShape
import com.iboalali.basicrootchecker.ui.rememberHapticClick
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import com.iboalali.basicrootchecker.util.DeviceInfo
import com.iboalali.basicrootchecker.util.PreviewLocales
import com.iboalali.basicrootchecker.util.openPlayStoreListing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    // Read-only: the catalog fetch is owned by MainActivity (kicked off at app start). This screen
    // just observes the already-loaded "Other apps" list.
    val viewModel: AboutViewModel = viewModel()
    val otherApps by viewModel.otherApps.collectAsStateWithLifecycle()
    AboutScreenContent(otherApps = otherApps, onNavigateBack = onNavigateBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreenContent(
    otherApps: ImmutableList<OtherAppUi>,
    onNavigateBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    val openUri: (String, String) -> Unit = { platform, uri ->
        Analytics.trackSocialLinkClicked(platform)
        context.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
    }

    // True on Google Play builds (false on FOSS, where there's no Play Store to rate on). Safe cast
    // so Compose previews — whose context isn't the app's Application — fall back to hidden.
    val rateAvailable = remember {
        (context.applicationContext as? BasicRootCheckerApplication)?.reviewController?.isAvailable == true
    }

    Scaffold(
        modifier = Modifier
            .detailDialogShape()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.action_about)) },
                navigationIcon = {
                    // Back-arrow when pushed full-screen; a close (X) when shown as a dialog over
                    // the main screen on large screens (see LocalDetailNavIcon).
                    val navIcon = LocalDetailNavIcon.current
                    IconButton(onClick = rememberHapticClick(onNavigateBack)) {
                        Icon(
                            painter = painterResource(
                                if (navIcon == DetailNavIcon.CLOSE) R.drawable.close_24px
                                else R.drawable.arrow_back_24px,
                            ),
                            contentDescription = stringResource(
                                if (navIcon == DetailNavIcon.CLOSE) R.string.content_description_close
                                else R.string.content_description_navigate_up,
                            ),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding(),
            start = innerPadding.calculateLeftPadding(layoutDirection),
            end = innerPadding.calculateRightPadding(layoutDirection),
        )
        val bottomPadding = innerPadding.calculateBottomPadding()
        Column(
            modifier = Modifier
                .testTag("about_list")
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            OutlinedCard(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_baseline_tag_24),
                            contentDescription = stringResource(R.string.contentDescription_appIcon),
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Text(
                                text = DeviceInfo.getAppVersionName(context),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.about_part1),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Spacer(Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        SocialLinkRow(
                            iconRes = R.drawable.alternate_email_24px,
                            label = stringResource(R.string.about_link_email),
                            handle = "contact@iboalali.com",
                            onClick = { openUri("email", "mailto:contact@iboalali.com") },
                        )
                        SocialLinkDivider()
                        SocialLinkRow(
                            iconRes = R.drawable.mastodon,
                            label = stringResource(R.string.about_link_mastodon),
                            handle = "@iboalali@mastodon.social",
                            onClick = { openUri("mastodon", "https://mastodon.social/@iboalali") },
                        )
                        SocialLinkDivider()
                        SocialLinkRow(
                            iconRes = R.drawable.bluesky,
                            label = stringResource(R.string.about_link_bluesky),
                            handle = "@iboalali.bsky.social",
                            onClick = { openUri("bluesky", "https://bsky.app/profile/iboalali.bsky.social") },
                        )
                        SocialLinkDivider()
                        SocialLinkRow(
                            iconRes = R.drawable.public_24px,
                            label = stringResource(R.string.about_link_website),
                            handle = "iboalali.com",
                            onClick = {
                                openUri(
                                    "website",
                                    "https://iboalali.com/?utm_source=android_app&utm_campaign=basic_root_checker&utm_content=home",
                                )
                            },
                        )
                        if (rateAvailable) {
                            SocialLinkDivider()
                            SocialLinkRow(
                                iconRes = R.drawable.star_24px,
                                label = stringResource(R.string.about_link_rate_this_app),
                                handle = stringResource(R.string.about_link_rate_subtitle),
                                onClick = {
                                    Analytics.trackRateLinkClicked()
                                    context.openPlayStoreListing()
                                },
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.about_not_affiliated),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            OtherAppsCard(
                apps = otherApps,
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp + bottomPadding))
        }
    }
}

@Composable
private fun SocialLinkRow(
    iconRes: Int,
    label: String,
    handle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = rememberHapticClick(onClick))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = handle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SocialLinkDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@PreviewLightDark
@PreviewDynamicColors
@PreviewScreenSizes
@PreviewLocales
@PreviewFontScale
@Composable
private fun AboutScreenPreview() {
    BasicRootCheckerTheme {
        AboutScreenContent(
            otherApps = persistentListOf(
                OtherAppUi(
                    name = "Billboard",
                    description = "Show large text on screen, as big as possible without cutting it off.",
                    iconUrl = null,
                    website = "https://iboalali.com/app/billboard/",
                    packageName = "com.iboalali.billboard",
                    whatsNew = persistentListOf("New **dark theme** and bigger text scaling"),
                ),
                OtherAppUi(
                    name = "Icon Recomposer",
                    description = "Light vector icons with a movable 3D emboss, then export to PNG, SVG, or VectorDrawable.",
                    iconUrl = null,
                    website = "https://iboalali.com/Icon-Recomposer/",
                    packageName = null,
                    whatsNew = persistentListOf("Your work is **saved automatically** and restored when you return"),
                ),
            ),
            onNavigateBack = {},
        )
    }
}
