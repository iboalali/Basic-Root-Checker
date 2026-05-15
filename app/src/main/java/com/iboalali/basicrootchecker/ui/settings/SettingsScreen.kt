package com.iboalali.basicrootchecker.ui.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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
import com.iboalali.basicrootchecker.R
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import com.iboalali.basicrootchecker.util.PreviewLocales

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val telemetryEnabled by viewModel.telemetryEnabled.collectAsStateWithLifecycle()

    SettingsScreenContent(
        telemetryEnabled = telemetryEnabled,
        onTelemetryEnabledChange = viewModel::setTelemetryEnabled,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    telemetryEnabled: Boolean,
    onTelemetryEnabledChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.action_settings)) },
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
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_telemetry_title),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.settings_telemetry_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Switch(
                        checked = telemetryEnabled,
                        onCheckedChange = onTelemetryEnabledChange,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedCard(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Analytics.trackPrivacyPolicyClicked()
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://iboalali.com/app/basic_root_checker/?utm_source=android_app&utm_campaign=basic_root_checker&utm_content=privacy#privacy-policy".toUri(),
                                )
                            )
                        }
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.action_privacy_policy),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.settings_privacy_policy_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Icon(
                        painter = painterResource(R.drawable.open_in_new_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

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
private fun SettingsScreenPreview() {
    BasicRootCheckerTheme {
        SettingsScreenContent(
            telemetryEnabled = true,
            onTelemetryEnabledChange = {},
            onNavigateBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsScreenTelemetryOffPreview() {
    BasicRootCheckerTheme {
        SettingsScreenContent(
            telemetryEnabled = false,
            onTelemetryEnabledChange = {},
            onNavigateBack = {},
        )
    }
}
