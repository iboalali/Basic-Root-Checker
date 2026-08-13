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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.iboalali.basicrootchecker.billing.TipTier
import com.iboalali.basicrootchecker.data.ThemeMode
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import com.iboalali.basicrootchecker.ui.tip.TipJarDialog
import com.iboalali.basicrootchecker.util.AppLanguage
import com.iboalali.basicrootchecker.util.PreviewLocales
import com.iboalali.haptics.compose.rememberHapticClick
import com.iboalali.haptics.compose.rememberHapticToggle
import com.iboalali.nav3.overlay.DetailNavigationIcon
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.launch

/** Outer (rounded) corner radius for the first/last item of the settings group. */
private val SettingsGroupCornerRadius = 32.dp

/** Inner corner radius where items connect within the settings group. */
private val SettingsItemInnerRadius = 2.dp

/** Gap between connected items in the settings group. */
private val SettingsItemSpacing = 4.dp

/**
 * Builds the corner shape for one item in the settings group so the items read as a single
 * connected list: rounded outer corners on the first/last item, near-square corners where items
 * meet.
 */
internal fun settingsGroupShape(isFirst: Boolean, isLast: Boolean) =
    RoundedCornerShape(
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
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val currentLanguageTag = AppLanguage.currentTag(LocalContext.current)
    val tipProducts by viewModel.tipProducts.collectAsStateWithLifecycle()
    val supporterTiers by viewModel.supporterTiers.collectAsStateWithLifecycle()

    SettingsScreenContent(
        telemetryEnabled = telemetryEnabled,
        onTelemetryEnabledChange = viewModel::setTelemetryEnabled,
        onResetIdentity = viewModel::resetTelemetryIdentity,
        hapticsEnabled = hapticsEnabled,
        onHapticsEnabledChange = viewModel::setHapticsEnabled,
        themeMode = themeMode,
        onThemeModeChange = viewModel::setThemeMode,
        currentLanguageTag = currentLanguageTag,
        onLanguageSelected = viewModel::setLanguage,
        tipJarAvailable = viewModel.tipJarAvailable,
        tipProducts = tipProducts,
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
    onResetIdentity: () -> Unit,
    hapticsEnabled: Boolean,
    onHapticsEnabledChange: (Boolean) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    currentLanguageTag: String?,
    onLanguageSelected: (String?) -> Unit,
    tipJarAvailable: Boolean,
    tipProducts: ImmutableList<TipProduct>,
    supporterTiers: ImmutableSet<TipTier>,
    onTipJarOpened: () -> Unit,
    onTipSelected: (TipTier) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTipDialog by remember { mutableStateOf(false) }
    var showResetIdentityDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resetIdentityDoneMessage = stringResource(R.string.settings_reset_identity_done)

    // Tip outcomes (thanks / pending / error) are announced app-wide by AppRoot, not here: the
    // billing events flow is single-consumer, and at expanded width this screen is composed as an
    // overlay *over* a live MainScreen, so a second collector would split the events between them.

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.action_settings)) },
                // Back-arrow when pushed full-screen; a close (X) when shown as a dialog over the
                // main screen on large screens (see LocalDetailNavIcon). Glyphs and content
                // descriptions come from LocalDetailOverlayStyle, and the haptic tap is the shared
                // component's — onNavigateBack goes in unwrapped.
                navigationIcon = { DetailNavigationIcon(onBack = onNavigateBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.testTag("settings_list")
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            if (tipJarAvailable) {
                OutlinedCard(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(),
                    shape = settingsGroupShape(isFirst = true, isLast = false),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                ) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable(
                                    onClick =
                                        rememberHapticClick {
                                            onTipJarOpened()
                                            showTipDialog = true
                                        }
                                )
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
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(),
                shape = settingsGroupShape(isFirst = !tipJarAvailable, isLast = false),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .toggleable(
                                value = telemetryEnabled,
                                onValueChange = rememberHapticToggle(onTelemetryEnabledChange),
                                role = Role.Switch,
                            )
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
                    // The row owns the toggle (Role.Switch) so the control is labeled by
                    // the title for screen readers; the Switch itself is non-interactive.
                    Switch(
                        checked = telemetryEnabled,
                        onCheckedChange = null,
                    )
                }
            }

            if (telemetryEnabled) {
                Spacer(Modifier.height(SettingsItemSpacing))

                OutlinedCard(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(),
                    shape = settingsGroupShape(isFirst = false, isLast = false),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                ) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable(
                                    onClick = rememberHapticClick { showResetIdentityDialog = true }
                                )
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_reset_identity_title),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = stringResource(R.string.settings_reset_identity_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(SettingsItemSpacing))

            OutlinedCard(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(),
                shape = settingsGroupShape(isFirst = false, isLast = false),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .toggleable(
                                value = hapticsEnabled,
                                onValueChange = rememberHapticToggle(onHapticsEnabledChange),
                                role = Role.Switch,
                            )
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
                    // The row owns the toggle (Role.Switch) so the control is labeled by
                    // the title for screen readers; the Switch itself is non-interactive.
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = null,
                    )
                }
            }

            Spacer(Modifier.height(SettingsItemSpacing))

            OutlinedCard(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(),
                shape = settingsGroupShape(isFirst = false, isLast = false),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable(onClick = rememberHapticClick { showThemeDialog = true })
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_theme_title),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = themeModeLabel(themeMode),
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

            if (AppLanguage.isSupported) {
                Spacer(Modifier.height(SettingsItemSpacing))

                OutlinedCard(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(),
                    shape = settingsGroupShape(isFirst = false, isLast = false),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                ) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable(
                                    onClick = rememberHapticClick { showLanguageDialog = true }
                                )
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_language_title),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text =
                                    currentLanguageTag?.let { AppLanguage.displayName(it) }
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
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(),
                shape = settingsGroupShape(isFirst = false, isLast = !BuildConfig.DEBUG),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable(
                                onClick =
                                    rememberHapticClick {
                                        Analytics.trackPrivacyPolicyClicked()
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                "https://iboalali.com/app/basic_root_checker/privacy?utm_source=android_app&utm_campaign=basic_root_checker&utm_content=privacy"
                                                    .toUri(),
                                            )
                                        )
                                    }
                            )
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

    if (showThemeDialog) {
        ThemePickerDialog(
            current = themeMode,
            onSelect = {
                showThemeDialog = false
                onThemeModeChange(it)
            },
            onDismiss = { showThemeDialog = false },
        )
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
            onSelect = {
                showTipDialog = false
                onTipSelected(it)
            },
            onDismiss = { showTipDialog = false },
        )
    }

    if (showResetIdentityDialog) {
        AlertDialog(
            onDismissRequest = { showResetIdentityDialog = false },
            title = { Text(stringResource(R.string.settings_reset_identity_title)) },
            text = { Text(stringResource(R.string.settings_reset_identity_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick =
                        rememberHapticClick {
                            showResetIdentityDialog = false
                            onResetIdentity()
                            scope.launch {
                                snackbarHostState.showSnackbar(resetIdentityDoneMessage)
                            }
                        }
                ) {
                    Text(stringResource(R.string.settings_reset_identity_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = rememberHapticClick { showResetIdentityDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
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
                SettingOptionRow(
                    label = stringResource(R.string.language_system_default),
                    selected = currentTag == null,
                    onClick = { onSelect(null) },
                )
                AppLanguage.SUPPORTED_TAGS.forEach { tag ->
                    SettingOptionRow(
                        label = AppLanguage.displayName(tag),
                        selected = currentTag == tag,
                        onClick = { onSelect(tag) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = rememberHapticClick(onDismiss)) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String =
    stringResource(
        when (mode) {
            ThemeMode.SYSTEM -> R.string.theme_follow_system
            ThemeMode.LIGHT -> R.string.theme_light
            ThemeMode.DARK -> R.string.theme_dark
        }
    )

@Composable
private fun ThemePickerDialog(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme_title)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    SettingOptionRow(
                        label = themeModeLabel(mode),
                        selected = current == mode,
                        onClick = { onSelect(mode) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = rememberHapticClick(onDismiss)) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun SettingOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .selectable(
                    selected = selected,
                    onClick = rememberHapticClick(onClick),
                    role = Role.RadioButton,
                )
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
            onResetIdentity = {},
            hapticsEnabled = true,
            onHapticsEnabledChange = {},
            themeMode = ThemeMode.SYSTEM,
            onThemeModeChange = {},
            currentLanguageTag = "de",
            onLanguageSelected = {},
            tipJarAvailable = true,
            tipProducts =
                persistentListOf(
                    TipProduct(TipTier.SMALL, "$1.99"),
                    TipProduct(TipTier.MEDIUM, "$4.99"),
                    TipProduct(TipTier.LARGE, "$9.99"),
                ),
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
            onResetIdentity = {},
            hapticsEnabled = false,
            onHapticsEnabledChange = {},
            themeMode = ThemeMode.DARK,
            onThemeModeChange = {},
            currentLanguageTag = null,
            onLanguageSelected = {},
            tipJarAvailable = false,
            tipProducts = persistentListOf(),
            supporterTiers = persistentSetOf(),
            onTipJarOpened = {},
            onTipSelected = {},
            onNavigateBack = {},
        )
    }
}
