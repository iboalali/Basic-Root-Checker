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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iboalali.basicrootchecker.BuildConfig
import com.iboalali.basicrootchecker.R
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.billing.TipProduct
import com.iboalali.basicrootchecker.billing.TipPurchaseState
import com.iboalali.basicrootchecker.billing.TipTier
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import com.iboalali.basicrootchecker.util.AppLanguage
import com.iboalali.basicrootchecker.util.PreviewLocales

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val telemetryEnabled by viewModel.telemetryEnabled.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val currentLanguageTag = AppLanguage.currentTag(LocalContext.current)
    val tipProducts by viewModel.tipProducts.collectAsStateWithLifecycle()
    val tipPurchaseState by viewModel.tipPurchaseState.collectAsStateWithLifecycle()
    val supporterTiers by viewModel.supporterTiers.collectAsStateWithLifecycle()

    SettingsScreenContent(
        telemetryEnabled = telemetryEnabled,
        onTelemetryEnabledChange = viewModel::setTelemetryEnabled,
        hapticsEnabled = hapticsEnabled,
        onHapticsEnabledChange = viewModel::setHapticsEnabled,
        currentLanguageTag = currentLanguageTag,
        onLanguageSelected = viewModel::setLanguage,
        tipJarAvailable = viewModel.tipJarAvailable,
        tipProducts = tipProducts,
        tipPurchaseState = tipPurchaseState,
        supporterTiers = supporterTiers,
        onTipJarOpened = viewModel::onTipJarOpened,
        onTipSelected = viewModel::onTipSelected,
        onTipResultShown = viewModel::onTipResultShown,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    telemetryEnabled: Boolean,
    onTelemetryEnabledChange: (Boolean) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsEnabledChange: (Boolean) -> Unit,
    currentLanguageTag: String?,
    onLanguageSelected: (String?) -> Unit,
    tipJarAvailable: Boolean,
    tipProducts: List<TipProduct>,
    tipPurchaseState: TipPurchaseState,
    supporterTiers: Set<TipTier>,
    onTipJarOpened: () -> Unit,
    onTipSelected: (TipTier) -> Unit,
    onTipResultShown: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTipDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val tipThanksMessage = stringResource(R.string.tip_jar_thanks)
    val tipErrorMessage = stringResource(R.string.tip_jar_error)
    LaunchedEffect(tipPurchaseState) {
        when (tipPurchaseState) {
            TipPurchaseState.Thanks -> {
                showTipDialog = false
                snackbarHostState.showSnackbar(tipThanksMessage)
                onTipResultShown()
            }
            TipPurchaseState.Error -> {
                snackbarHostState.showSnackbar(tipErrorMessage)
                onTipResultShown()
            }
            else -> Unit
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_haptics_title),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.settings_haptics_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = onHapticsEnabledChange,
                    )
                }
            }

            if (AppLanguage.isSupported) {
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
                            .clickable { showLanguageDialog = true }
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_language_title),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = currentLanguageTag?.let { AppLanguage.displayName(it) }
                                    ?: stringResource(R.string.language_system_default),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Icon(
                            painter = painterResource(R.drawable.chevron_right_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (tipJarAvailable) {
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
                                onTipJarOpened()
                                showTipDialog = true
                            }
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_tip_jar_title),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = stringResource(R.string.settings_tip_jar_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Icon(
                            painter = painterResource(R.drawable.chevron_right_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                                    "https://iboalali.com/app/basic_root_checker/privacy?utm_source=android_app&utm_campaign=basic_root_checker&utm_content=privacy".toUri(),
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

            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(24.dp))
                DebugTipJarCard(supporterTiers = supporterTiers)
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(
            currentTag = currentLanguageTag,
            onSelect = {
                showLanguageDialog = false
                onLanguageSelected(it)
            },
            onDismiss = { showLanguageDialog = false },
        )
    }

    if (showTipDialog) {
        TipJarDialog(
            products = tipProducts,
            onSelect = onTipSelected,
            onDismiss = { showTipDialog = false },
        )
    }
}

@Composable
private fun TipJarDialog(
    products: List<TipProduct>,
    onSelect: (TipTier) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tip_jar_dialog_title)) },
        text = {
            if (products.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.tip_jar_loading),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                Column {
                    products.forEach { product ->
                        TextButton(
                            onClick = { onSelect(product.tier) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(product.tier.titleRes),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = product.formattedPrice,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun LanguagePickerDialog(
    currentTag: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language_title)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                LanguageOptionRow(
                    label = stringResource(R.string.language_system_default),
                    selected = currentTag == null,
                    onClick = { onSelect(null) },
                )
                AppLanguage.SUPPORTED_TAGS.forEach { tag ->
                    LanguageOptionRow(
                        label = AppLanguage.displayName(tag),
                        selected = currentTag == tag,
                        onClick = { onSelect(tag) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun LanguageOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
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
            hapticsEnabled = true,
            onHapticsEnabledChange = {},
            currentLanguageTag = "de",
            onLanguageSelected = {},
            tipJarAvailable = true,
            tipProducts = listOf(
                TipProduct(TipTier.SMALL, "$1.99"),
                TipProduct(TipTier.MEDIUM, "$4.99"),
                TipProduct(TipTier.LARGE, "$9.99"),
            ),
            tipPurchaseState = TipPurchaseState.Idle,
            supporterTiers = setOf(TipTier.SMALL),
            onTipJarOpened = {},
            onTipSelected = {},
            onTipResultShown = {},
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
            hapticsEnabled = false,
            onHapticsEnabledChange = {},
            currentLanguageTag = null,
            onLanguageSelected = {},
            tipJarAvailable = false,
            tipProducts = emptyList(),
            tipPurchaseState = TipPurchaseState.Idle,
            supporterTiers = emptySet(),
            onTipJarOpened = {},
            onTipSelected = {},
            onTipResultShown = {},
            onNavigateBack = {},
        )
    }
}
