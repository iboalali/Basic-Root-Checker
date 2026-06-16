package com.iboalali.basicrootchecker.ui.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
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
import androidx.compose.material3.Surface
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iboalali.basicrootchecker.BuildConfig
import com.iboalali.basicrootchecker.R
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.billing.TipEvent
import com.iboalali.basicrootchecker.billing.TipProduct
import com.iboalali.basicrootchecker.billing.TipTier
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import com.iboalali.basicrootchecker.util.AppLanguage
import com.iboalali.basicrootchecker.util.PreviewLocales
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Outer (rounded) corner radius for the first/last item of the settings group. */
private val SettingsGroupCornerRadius = 32.dp

/** Inner corner radius where items connect within the settings group. */
private val SettingsItemInnerRadius = 2.dp

/** Gap between connected items in the settings group. */
private val SettingsItemSpacing = 4.dp

/**
 * Builds the corner shape for one item in the settings group so the items read as a single
 * connected list: rounded outer corners on the first/last item, near-square corners where
 * items meet.
 */
internal fun settingsGroupShape(isFirst: Boolean, isLast: Boolean) = RoundedCornerShape(
    topStart = if (isFirst) SettingsGroupCornerRadius else SettingsItemInnerRadius,
    topEnd = if (isFirst) SettingsGroupCornerRadius else SettingsItemInnerRadius,
    bottomStart = if (isLast) SettingsGroupCornerRadius else SettingsItemInnerRadius,
    bottomEnd = if (isLast) SettingsGroupCornerRadius else SettingsItemInnerRadius,
)

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val telemetryEnabled by viewModel.telemetryEnabled.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val currentLanguageTag = AppLanguage.currentTag(LocalContext.current)
    val tipProducts by viewModel.tipProducts.collectAsStateWithLifecycle()
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
        tipEvents = viewModel.tipEvents,
        supporterTiers = supporterTiers,
        onTipJarOpened = viewModel::onTipJarOpened,
        onTipSelected = viewModel::onTipSelected,
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
    tipProducts: ImmutableList<TipProduct>,
    tipEvents: Flow<TipEvent>,
    supporterTiers: ImmutableSet<TipTier>,
    onTipJarOpened: () -> Unit,
    onTipSelected: (TipTier) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTipDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val tipThanksMessage = stringResource(R.string.tip_jar_thanks)
    val tipPendingMessage = stringResource(R.string.tip_jar_pending)
    val tipErrorMessage = stringResource(R.string.tip_jar_error)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(tipEvents, lifecycleOwner) {
        // One-shot events: collect only while at least STARTED, so a snackbar can't fire
        // for an event delivered while the screen is in the background.
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            tipEvents.collect { event ->
                when (event) {
                    TipEvent.Thanks -> {
                        showTipDialog = false
                        snackbarHostState.showSnackbar(tipThanksMessage)
                    }
                    TipEvent.Pending -> {
                        showTipDialog = false
                        snackbarHostState.showSnackbar(tipPendingMessage)
                    }
                    TipEvent.Error -> snackbarHostState.showSnackbar(tipErrorMessage)
                }
            }
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

            if (tipJarAvailable) {
                OutlinedCard(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(),
                    shape = settingsGroupShape(isFirst = true, isLast = false),
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

                Spacer(Modifier.height(SettingsItemSpacing))
            }

            OutlinedCard(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(),
                shape = settingsGroupShape(isFirst = !tipJarAvailable, isLast = false),
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

            Spacer(Modifier.height(SettingsItemSpacing))

            OutlinedCard(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(),
                shape = settingsGroupShape(isFirst = false, isLast = false),
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
                Spacer(Modifier.height(SettingsItemSpacing))

                OutlinedCard(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(),
                    shape = settingsGroupShape(isFirst = false, isLast = false),
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

            Spacer(Modifier.height(SettingsItemSpacing))

            OutlinedCard(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(),
                shape = settingsGroupShape(isFirst = false, isLast = !BuildConfig.DEBUG),
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
                Spacer(Modifier.height(SettingsItemSpacing))
                DebugTipJarCard(
                    supporterTiers = supporterTiers,
                    shape = settingsGroupShape(isFirst = false, isLast = true),
                )
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
    products: ImmutableList<TipProduct>,
    onSelect: (TipTier) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tip_jar_dialog_title)) },
        text = { TipJarTiers(products = products, onSelect = onSelect) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun TipJarTiers(
    products: ImmutableList<TipProduct>,
    onSelect: (TipTier) -> Unit,
) {
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            products.forEach { product ->
                Card(
                    onClick = { onSelect(product.tier) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(product.tier.titleRes),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = product.formattedPrice,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mimics the [AlertDialog] surface so the tip-jar layout renders in the IDE preview.
 * A real [AlertDialog] draws inside a [androidx.compose.ui.window.Dialog] window, which the
 * Compose preview renderer shows as blank — so the preview reuses [TipJarTiers] inside a
 * plain dialog-shaped [Surface] instead.
 */
@Composable
private fun TipJarDialogPreviewSurface(products: ImmutableList<TipProduct>) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.padding(16.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.tip_jar_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(16.dp))
            TipJarTiers(products = products, onSelect = {})
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {}, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    }
}

@PreviewLightDark
@PreviewDynamicColors
@Composable
private fun TipJarDialogPreview() {
    BasicRootCheckerTheme {
        TipJarDialogPreviewSurface(
            products = persistentListOf(
                TipProduct(TipTier.SMALL, "$1.99"),
                TipProduct(TipTier.MEDIUM, "$4.99"),
                TipProduct(TipTier.LARGE, "$9.99"),
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun TipJarDialogLoadingPreview() {
    BasicRootCheckerTheme {
        TipJarDialogPreviewSurface(products = persistentListOf())
    }
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
            tipProducts = persistentListOf(
                TipProduct(TipTier.SMALL, "$1.99"),
                TipProduct(TipTier.MEDIUM, "$4.99"),
                TipProduct(TipTier.LARGE, "$9.99"),
            ),
            tipEvents = emptyFlow(),
            supporterTiers = persistentSetOf(TipTier.SMALL),
            onTipJarOpened = {},
            onTipSelected = {},
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
            tipProducts = persistentListOf(),
            tipEvents = emptyFlow(),
            supporterTiers = persistentSetOf(),
            onTipJarOpened = {},
            onTipSelected = {},
            onNavigateBack = {},
        )
    }
}
