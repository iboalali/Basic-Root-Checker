package com.iboalali.basicrootchecker.ui.about

import android.content.Intent
import android.text.util.Linkify
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.iboalali.basicrootchecker.R
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import com.iboalali.basicrootchecker.util.DeviceInfo
import com.iboalali.basicrootchecker.util.PreviewLocales
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.action_about)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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

                    Spacer(Modifier.height(8.dp))

                    AndroidView(
                        factory = { ctx ->
                            TextView(ctx).apply {
                                text = ctx.getString(R.string.about_email_socials)
                                @Suppress("DEPRECATION")
                                autoLinkMask = Linkify.ALL
                                setTextColor(textColor)
                                setLinkTextColor(linkColor)
                                textSize = 14f
                                setPadding(0, 0, 0, 0)
                            }
                        },
                        modifier = Modifier.padding(start = 32.dp),
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Analytics.trackWebsiteClicked()
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        "https://iboalali.com/?utm_source=android_app&utm_campaign=basic_root_checker&utm_content=home".toUri(),
                                    )
                                )
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.about_visit_website),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(R.drawable.open_in_new_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
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
                apps = persistentListOf(
                    OtherApp(
                        name = "Billboard",
                        descriptionRes = R.string.other_apps_billboard_description,
                        iconRes = R.mipmap.billboard_app_icon,
                        packageName = "com.iboalali.billboard",
                    ),
                    OtherApp(
                        name = "Hide Persistent Notifications",
                        descriptionRes = R.string.other_apps_hide_notifications_description,
                        iconRes = R.mipmap.hide_persistent_notification_app_icon,
                        packageName = "com.iboalali.hidepersistentnotifications",
                    ),
                ),
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@PreviewLightDark
@PreviewDynamicColors
@PreviewScreenSizes
@PreviewLocales
@PreviewFontScale
@Composable
private fun AboutScreenPreview() {
    BasicRootCheckerTheme {
        AboutScreen(onNavigateBack = {})
    }
}
