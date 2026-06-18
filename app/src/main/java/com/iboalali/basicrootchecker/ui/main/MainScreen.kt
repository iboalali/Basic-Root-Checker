package com.iboalali.basicrootchecker.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iboalali.basicrootchecker.BuildConfig
import com.iboalali.basicrootchecker.R
import com.iboalali.basicrootchecker.data.RootManager
import com.iboalali.basicrootchecker.data.RootProvider
import com.iboalali.basicrootchecker.data.RootResult
import com.iboalali.basicrootchecker.ui.components.AppBarDropdownMenuItem
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import com.iboalali.basicrootchecker.update.AppUpdateEvent
import com.iboalali.basicrootchecker.util.PreviewLocales
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onNavigateToAbout: () -> Unit,
    onNavigateToLicence: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MainScreenContent(
        uiState = uiState,
        onCheckRoot = viewModel::checkRoot,
        onRequestRoot = viewModel::requestRoot,
        onCheckRootDemo = viewModel::checkRootDemo,
        onDemoUpdateChoice = { choice ->
            when (choice) {
                DebugUpdateChoice.AVAILABLE -> viewModel.demoUpdate(AppUpdateEvent.Available)
                DebugUpdateChoice.DOWNLOADING -> viewModel.demoUpdateDownloading()
                DebugUpdateChoice.DOWNLOADED -> viewModel.demoUpdate(AppUpdateEvent.Downloaded)
                DebugUpdateChoice.FAILED -> viewModel.demoUpdate(AppUpdateEvent.Failed(-100))
                DebugUpdateChoice.RESET -> viewModel.demoUpdate(AppUpdateEvent.None)
            }
        },
        onUpdateRequested = viewModel::onUpdateRequested,
        onInstallRequested = viewModel::onInstallRequested,
        onAppUpdatedSnackbarShown = viewModel::onAppUpdatedSnackbarShown,
        onNavigateToAbout = onNavigateToAbout,
        onNavigateToLicence = onNavigateToLicence,
        onNavigateToSettings = onNavigateToSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreenContent(
    uiState: MainUiState,
    onCheckRoot: () -> Unit,
    onRequestRoot: () -> Unit,
    onUpdateRequested: () -> Unit,
    onInstallRequested: () -> Unit,
    onAppUpdatedSnackbarShown: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToLicence: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onCheckRootDemo: (RootResult) -> Unit = {},
    onDemoUpdateChoice: (DebugUpdateChoice) -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    var showDemoDialog by remember { mutableStateOf(false) }
    var showUpdateDemoDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val checkingText = stringResource(R.string.string_checking_for_root)
    val copiedText = stringResource(R.string.toast_content_copied)
    val appUpdatedText = stringResource(R.string.app_updated_snackbar)

    LaunchedEffect(uiState.appUpdatedShown) {
        if (uiState.appUpdatedShown) {
            snackbarHostState.showSnackbar(appUpdatedText)
            onAppUpdatedSnackbarShown()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        textAlign = TextAlign.Center,
                    )
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert_24px),
                            contentDescription = stringResource(R.string.content_description_more_options),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        AppBarDropdownMenuItem(
                            text = stringResource(R.string.action_licence),
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_baseline_text_snippet_24),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onNavigateToLicence()
                            },
                        )
                        AppBarDropdownMenuItem(
                            text = stringResource(R.string.action_settings),
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.settings_24px),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onNavigateToSettings()
                            },
                        )
                        AppBarDropdownMenuItem(
                            text = stringResource(R.string.action_about),
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_baseline_android_24),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onNavigateToAbout()
                            },
                        )
                        if (BuildConfig.DEBUG) {
                            AppBarDropdownMenuItem(
                                text = "Demo: in-app update",
                                onClick = {
                                    menuExpanded = false
                                    showUpdateDemoDialog = true
                                },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (BuildConfig.DEBUG) {
                        showDemoDialog = true
                    } else {
                        onCheckRoot()
                        scope.launch { snackbarHostState.showSnackbar(checkingText) }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_tag_24),
                    contentDescription = stringResource(R.string.content_description_check_for_root),
                )
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(16.dp),
                )
            }
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val topPadding = innerPadding.calculateTopPadding()
        val bottomPadding = innerPadding.calculateBottomPadding()
        val leftPadding = innerPadding.calculateLeftPadding(layoutDirection)
        val rightPadding = innerPadding.calculateRightPadding(layoutDirection)
        val contentPadding =
            PaddingValues(top = topPadding, start = leftPadding, end = rightPadding)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            // Root Status Card
            OutlinedCard(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AnimatedContent(
                        targetState = uiState.rootStatus,
                        transitionSpec = {
                            (fadeIn(tween(300)) + scaleIn(
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                                initialScale = 0.6f,
                            )).togetherWith(
                                fadeOut(tween(200)) + scaleOut(
                                    tween(200),
                                    targetScale = 0.6f,
                                ),
                            )
                        },
                        label = "statusIcon",
                    ) { status ->
                        Box(
                            modifier = Modifier.size(96.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            when (status) {
                                RootStatus.CHECKING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(88.dp),
                                        strokeWidth = 6.dp,
                                    )
                                }

                                else -> {
                                    val imageRes = when (status) {
                                        RootStatus.ROOTED -> R.drawable.ic_success_c
                                        RootStatus.NOT_ROOTED, RootStatus.UNKNOWN -> R.drawable.ic_fail_c
                                        RootStatus.NOT_GRANTED -> R.drawable.ic_unknown_c
                                        else -> R.drawable.ic_unknown_c
                                    }
                                    val isResult = status == RootStatus.ROOTED ||
                                            status == RootStatus.NOT_ROOTED ||
                                            status == RootStatus.UNKNOWN ||
                                            status == RootStatus.NOT_GRANTED
                                    val scale = remember { Animatable(if (isResult) 0f else 1f) }
                                    LaunchedEffect(Unit) {
                                        if (isResult) {
                                            scale.animateTo(
                                                targetValue = 1f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMediumLow,
                                                ),
                                            )
                                        }
                                    }
                                    Image(
                                        painter = painterResource(imageRes),
                                        contentDescription = stringResource(R.string.string_root_status_description),
                                        modifier = Modifier
                                            .size(96.dp)
                                            .graphicsLayer {
                                                scaleX = scale.value
                                                scaleY = scale.value
                                            },
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    AnimatedContent(
                        targetState = uiState.rootStatus,
                        transitionSpec = {
                            (fadeIn(tween(300))).togetherWith(fadeOut(tween(200)))
                        },
                        label = "statusText",
                    ) { status ->
                        Text(
                            text = when (status) {
                                RootStatus.NOT_CHECKED -> stringResource(R.string.textView_checkForRoot)
                                RootStatus.CHECKING -> stringResource(R.string.string_checking_for_root)
                                RootStatus.ROOTED -> stringResource(R.string.rootAvailable)
                                RootStatus.NOT_ROOTED -> stringResource(R.string.rootNotAvailable)
                                RootStatus.UNKNOWN -> stringResource(R.string.rootUnknown)
                                RootStatus.NOT_GRANTED -> stringResource(R.string.rootNotGranted)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            // Announce the result to screen readers when the status changes,
                            // since the FAB check updates this text without moving focus.
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }

                    // A bare "Other" (root present but no recognized manager) tells the user
                    // nothing, so suppress the line; a named OTHER-family manager (e.g. SuperSU)
                    // still shows.
                    val isGenericOther = uiState.rootManager == null &&
                            uiState.rootProvider == RootProvider.OTHER
                    val showProvider = (uiState.rootStatus == RootStatus.ROOTED ||
                            uiState.rootStatus == RootStatus.NOT_GRANTED) &&
                            uiState.rootProvider != RootProvider.UNKNOWN &&
                            !isGenericOther
                    if (showProvider) {
                        // Prefer the specific installed manager; fall back to the family name when
                        // only a mount/path/su signal identified it (no package).
                        val providerName = when (uiState.rootManager) {
                            RootManager.MAGISK -> stringResource(R.string.root_provider_magisk)
                            RootManager.KITSUNE_MASK -> stringResource(R.string.root_manager_kitsune_mask)
                            RootManager.KERNELSU -> stringResource(R.string.root_provider_kernelsu)
                            RootManager.KERNELSU_NEXT -> stringResource(R.string.root_manager_kernelsu_next)
                            RootManager.SUKISU_ULTRA -> stringResource(R.string.root_manager_sukisu_ultra)
                            RootManager.RESUKISU -> stringResource(R.string.root_manager_resukisu)
                            RootManager.APATCH -> stringResource(R.string.root_provider_apatch)
                            RootManager.SUPERSU -> stringResource(R.string.root_manager_supersu)
                            RootManager.SUPERUSER -> stringResource(R.string.root_manager_superuser)
                            RootManager.KINGROOT -> stringResource(R.string.root_manager_kingroot)
                            RootManager.PHH -> stringResource(R.string.root_manager_phh)
                            null -> when (uiState.rootProvider) {
                                RootProvider.MAGISK -> stringResource(R.string.root_provider_magisk)
                                RootProvider.KERNELSU -> stringResource(R.string.root_provider_kernelsu)
                                RootProvider.APATCH -> stringResource(R.string.root_provider_apatch)
                                RootProvider.OTHER -> stringResource(R.string.root_provider_other)
                                RootProvider.UNKNOWN -> ""
                            }
                        }
                        val version = uiState.rootProviderVersion
                        val providerText = if (version != null) {
                            stringResource(R.string.root_provider_via_with_version, providerName, version)
                        } else {
                            stringResource(R.string.root_provider_via, providerName)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = providerText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    if (uiState.rootStatus == RootStatus.NOT_ROOTED) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.root_hidden_manager_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    if (uiState.rootStatus == RootStatus.NOT_GRANTED ||
                        uiState.rootStatus == RootStatus.NOT_ROOTED
                    ) {
                        Spacer(Modifier.height(16.dp))
                        FilledTonalButton(onClick = onRequestRoot) {
                            Text(text = stringResource(R.string.action_request_root))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            if (uiState.updateStatus !is AppUpdateEvent.None) {
                // Update Card (hidden when updateStatus is None)
                UpdateCard(
                    updateStatus = uiState.updateStatus,
                    onUpdateClick = onUpdateRequested,
                    onInstallClick = onInstallRequested,
                )
                Spacer(Modifier.height(24.dp))
            }

            // Device Info Card
            OutlinedCard(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.string_your_device),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    DeviceInfoText(
                        label = stringResource(R.string.label_device_name),
                        text = uiState.deviceMarketingName,
                        contentDescription = stringResource(R.string.content_description_marketing_name),
                        onCopied = { scope.launch { snackbarHostState.showSnackbar(copiedText) } },
                    )
                    if (!uiState.deviceMarketingName.equals(uiState.deviceModelName, ignoreCase = true)) {
                        DeviceInfoText(
                            label = stringResource(R.string.label_model),
                            text = uiState.deviceModelName,
                            contentDescription = stringResource(R.string.content_description_model_name),
                            onCopied = { scope.launch { snackbarHostState.showSnackbar(copiedText) } },
                        )
                    }
                    DeviceInfoText(
                        label = stringResource(R.string.label_android_version),
                        text = uiState.androidVersion,
                        contentDescription = stringResource(R.string.content_description_android_version),
                        onCopied = { scope.launch { snackbarHostState.showSnackbar(copiedText) } },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Disclaimer Card
            OutlinedCard(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            ) {
                val htmlString = stringResource(R.string.textView_Disclaimer)
                val text = remember { AnnotatedString.fromHtml(htmlString) }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(24.dp),
                )
            }

            // 24dp spacing + FAB height (56dp) + FAB bottom margin (16dp) + system bottom padding
            Spacer(Modifier.height(24.dp + 56.dp + 16.dp + bottomPadding))
        }
    }

    if (BuildConfig.DEBUG && showDemoDialog) {
        DebugRootResultDialog(
            onSelectResult = { result ->
                showDemoDialog = false
                onCheckRootDemo(result)
                scope.launch { snackbarHostState.showSnackbar(checkingText) }
            },
            onRealCheck = {
                showDemoDialog = false
                onCheckRoot()
                scope.launch { snackbarHostState.showSnackbar(checkingText) }
            },
            onDismiss = { showDemoDialog = false },
        )
    }

    if (BuildConfig.DEBUG && showUpdateDemoDialog) {
        DebugUpdateDialog(
            onSelect = { choice ->
                showUpdateDemoDialog = false
                onDemoUpdateChoice(choice)
            },
            onDismiss = { showUpdateDemoDialog = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceInfoText(
    label: String,
    text: String,
    contentDescription: String,
    onCopied: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(contentDescription, text))
                    onCopied()
                },
            )
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenNotCheckedPreview() {
    BasicRootCheckerTheme {
        MainScreenContent(
            uiState = MainUiState(
                rootStatus = RootStatus.NOT_CHECKED,
                deviceMarketingName = "Pixel 8 Pro",
                deviceModelName = "husky",
                androidVersion = "Android 16",
            ),
            onCheckRoot = {},
            onRequestRoot = {},
            onUpdateRequested = {},
            onInstallRequested = {},
            onAppUpdatedSnackbarShown = {},
            onNavigateToAbout = {},
            onNavigateToLicence = {},
            onNavigateToSettings = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenCheckingPreview() {
    BasicRootCheckerTheme {
        MainScreenContent(
            uiState = MainUiState(
                rootStatus = RootStatus.CHECKING,
                deviceMarketingName = "Pixel 8 Pro",
                deviceModelName = "husky",
                androidVersion = "Android 16",
            ),
            onCheckRoot = {},
            onRequestRoot = {},
            onUpdateRequested = {},
            onInstallRequested = {},
            onAppUpdatedSnackbarShown = {},
            onNavigateToAbout = {},
            onNavigateToLicence = {},
            onNavigateToSettings = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenRootedPreview() {
    BasicRootCheckerTheme {
        MainScreenContent(
            uiState = MainUiState(
                rootStatus = RootStatus.ROOTED,
                rootProvider = RootProvider.MAGISK,
                rootProviderVersion = "27.0",
                deviceMarketingName = "Pixel 8 Pro",
                deviceModelName = "husky",
                androidVersion = "Android 16",
            ),
            onCheckRoot = {},
            onRequestRoot = {},
            onUpdateRequested = {},
            onInstallRequested = {},
            onAppUpdatedSnackbarShown = {},
            onNavigateToAbout = {},
            onNavigateToLicence = {},
            onNavigateToSettings = {},
        )
    }
}

@PreviewLocales
@Composable
private fun MainScreenLocalesPreview() {
    BasicRootCheckerTheme {
        MainScreenContent(
            uiState = MainUiState(
                rootStatus = RootStatus.NOT_CHECKED,
                deviceMarketingName = "Pixel 8 Pro",
                deviceModelName = "husky",
                androidVersion = "Android 16",
            ),
            onCheckRoot = {},
            onRequestRoot = {},
            onUpdateRequested = {},
            onInstallRequested = {},
            onAppUpdatedSnackbarShown = {},
            onNavigateToAbout = {},
            onNavigateToLicence = {},
            onNavigateToSettings = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenNotGrantedPreview() {
    BasicRootCheckerTheme {
        MainScreenContent(
            uiState = MainUiState(
                rootStatus = RootStatus.NOT_GRANTED,
                rootProvider = RootProvider.KERNELSU,
                rootManager = RootManager.SUKISU_ULTRA,
                deviceMarketingName = "Pixel 8 Pro",
                deviceModelName = "husky",
                androidVersion = "Android 16",
            ),
            onCheckRoot = {},
            onRequestRoot = {},
            onUpdateRequested = {},
            onInstallRequested = {},
            onAppUpdatedSnackbarShown = {},
            onNavigateToAbout = {},
            onNavigateToLicence = {},
            onNavigateToSettings = {},
        )
    }
}

@PreviewDynamicColors
@PreviewLightDark
@PreviewFontScale
@PreviewScreenSizes
@Composable
private fun MainScreenNotRootedPreview() {
    BasicRootCheckerTheme {
        MainScreenContent(
            uiState = MainUiState(
                rootStatus = RootStatus.NOT_ROOTED,
                deviceMarketingName = "Pixel 8 Pro",
                deviceModelName = "husky",
                androidVersion = "Android 16",
            ),
            onCheckRoot = {},
            onRequestRoot = {},
            onUpdateRequested = {},
            onInstallRequested = {},
            onAppUpdatedSnackbarShown = {},
            onNavigateToAbout = {},
            onNavigateToLicence = {},
            onNavigateToSettings = {},
        )
    }
}
