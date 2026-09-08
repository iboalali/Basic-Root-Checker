package com.iboalali.basicrootchecker.ui.main

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iboalali.basicrootchecker.BasicRootCheckerApplication
import com.iboalali.basicrootchecker.BuildConfig
import com.iboalali.basicrootchecker.R
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.billing.TipProduct
import com.iboalali.basicrootchecker.billing.TipTier
import com.iboalali.basicrootchecker.data.RootChecker
import com.iboalali.basicrootchecker.data.RootManager
import com.iboalali.basicrootchecker.data.RootProvider
import com.iboalali.basicrootchecker.data.RootResult
import com.iboalali.basicrootchecker.data.UserPreferences
import com.iboalali.basicrootchecker.update.AppUpdateEvent
import com.iboalali.basicrootchecker.util.DeviceInfo
import de.boehrsi.devicemarketingnames.DeviceMarketingNames
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val REVIEW_GATE_TAG = "ReviewGate"
private const val SUPPORT_GATE_TAG = "SupportGate"

/** Analytics label for tips started from the main screen's support card (vs. "settings"). */
private const val TIP_SOURCE_SUPPORT_CARD = "support_card"

enum class RootStatus {
    NOT_CHECKED,
    CHECKING,
    ROOTED,
    NOT_ROOTED,
    UNKNOWN,
    NOT_GRANTED,
}

data class MainUiState(
    val rootStatus: RootStatus = RootStatus.NOT_CHECKED,
    val rootProvider: RootProvider = RootProvider.UNKNOWN,
    val rootManager: RootManager? = null,
    val rootProviderVersion: String? = null,
    val deviceMarketingName: String = "",
    val deviceModelName: String = "",
    val androidVersion: String = "",
    val updateStatus: AppUpdateEvent = AppUpdateEvent.None,
    val appUpdatedShown: Boolean = false,
    /** Whether the tip-jar support card is offered (see [SupportGate]). */
    val supportPromptVisible: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appUpdateController =
        (application as BasicRootCheckerApplication).appUpdateController

    private val userPreferences = UserPreferences(application)

    private val haptics = (application as BasicRootCheckerApplication).rootHaptics

    private val reviewController = (application as BasicRootCheckerApplication).reviewController

    private val billing = (application as BasicRootCheckerApplication).billingController

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /** Tip offers for the support card's dialog. Empty in FOSS builds and until Play prices load. */
    val tipProducts: StateFlow<ImmutableList<TipProduct>> = billing.products

    init {
        loadDeviceInfo()
        viewModelScope.launch {
            appUpdateController.events.collect { event ->
                if (!demoUpdateActive) _uiState.update { it.copy(updateStatus = event) }
            }
        }
        viewModelScope.launch { checkForAppUpdate() }
    }

    private suspend fun checkForAppUpdate() {
        val stored = userPreferences.lastSeenVersionCode.first()
        val current = BuildConfig.VERSION_CODE
        if (stored in 1..<current) {
            _uiState.update { it.copy(appUpdatedShown = true) }
        }
        if (stored != current) {
            userPreferences.setLastSeenVersionCode(current)
        }
    }

    fun onAppUpdatedSnackbarShown() {
        _uiState.update { it.copy(appUpdatedShown = false) }
    }

    private fun loadDeviceInfo() {
        val app = getApplication<Application>()
        val resources = app.resources

        // A debug build may have been launched with a device armed for a recording; see
        // DemoDeviceOverride for why the emulator's own name is the wrong thing to film.
        val demoName = if (BuildConfig.DEBUG) DemoDeviceOverride.name else null
        val demoModel = if (BuildConfig.DEBUG) DemoDeviceOverride.model else null

        _uiState.update {
            it.copy(
                deviceMarketingName = demoName ?: DeviceMarketingNames.getSingleName(),
                deviceModelName = demoModel ?: Build.DEVICE,
                androidVersion = "${resources.getString(R.string.textViewAndroidVersion)} ${DeviceInfo.getAndroidVersionName()}",
            )
        }
    }

    fun checkRoot() {
        viewModelScope.launch {
            val hapticsOn = userPreferences.hapticsEnabled.first()
            startRootCheck()
            if (hapticsOn) haptics.startCheckingRamp()
            Analytics.trackRootCheckStarted()
            val result = RootChecker.check(getApplication())
            applyResult(result)
            if (hapticsOn) playResultHaptic(result)
            // Review first, support card second — and never both in one session (see SupportGate).
            maybeShowSupportPrompt(maybeRequestReview(result))
        }
    }

    fun requestRoot() {
        viewModelScope.launch {
            val hapticsOn = userPreferences.hapticsEnabled.first()
            startRootCheck()
            if (hapticsOn) haptics.startCheckingRamp()
            Analytics.trackRootRequested()
            val result = RootChecker.requestRoot(getApplication())
            applyResult(result)
            if (hapticsOn) playResultHaptic(result)
            // Review first, support card second — and never both in one session (see SupportGate).
            maybeShowSupportPrompt(maybeRequestReview(result))
        }
    }

    /**
     * After a root-found result, ask for an in-app rating once the gate opens (see [ReviewGate]).
     * Only [RootResult.Rooted] counts — confirming a device is rooted is the app's "win" moment.
     *
     * The version code is recorded — spending this release's single prompt — only once the request
     * actually reached Play, because Play's card is quota-limited and gives no "was it shown"
     * callback. A build without in-app review (FOSS) returns early, and a controller with no
     * attached activity reports `false`, so neither burns the slot or reports a prompt that never
     * happened.
     *
     * @return the rooted-check count this check produced, or `null` when the check counted toward
     *   neither post-check ask — not a root-found result, or a build without Play review (where the
     *   tip jar is absent too). [maybeShowSupportPrompt] consumes it instead of incrementing again.
     */
    private suspend fun maybeRequestReview(result: RootResult): Int? {
        if (result !is RootResult.Rooted) return null
        // Nothing to rate on without a Play Store, so don't even count toward the gate: the slot
        // stays unspent (and the counter untouched) if this install is ever replaced by a Play build.
        if (!reviewController.isAvailable) return null
        val rootedCount = userPreferences.incrementRootedCheckCount()
        val lastPromptedVersion = userPreferences.lastReviewPromptVersionCode.first()
        val currentVersion = BuildConfig.VERSION_CODE
        val shouldRequest = ReviewGate.shouldRequest(rootedCount, lastPromptedVersion, currentVersion)
        if (BuildConfig.DEBUG) {
            Log.d(
                REVIEW_GATE_TAG,
                "rootedCount=$rootedCount (need ${ReviewGate.MIN_ROOTED_CHECKS}), " +
                    "lastPromptedVersion=$lastPromptedVersion, currentVersion=$currentVersion " +
                    "-> shouldRequest=$shouldRequest",
            )
        }
        if (shouldRequest && reviewController.requestReview()) {
            userPreferences.setLastReviewPromptVersionCode(currentVersion)
            reviewRequestedThisSession = true
            Analytics.trackReviewRequested()
        }
        return rootedCount
    }

    /**
     * After a root-found result, offer the tip jar inline once [SupportGate] opens. Runs *after*
     * [maybeRequestReview] and takes the count it observed, so the two asks are ordered and the
     * rooted-check counter is incremented exactly once per check.
     *
     * Only sets the state; the card is drawn (and reports itself shown) by `MainScreen`, which also
     * yields the slot to a pending update card.
     */
    private suspend fun maybeShowSupportPrompt(rootedCount: Int?) {
        if (rootedCount == null) return
        val dismissCount = userPreferences.supportPromptDismissCount.first()
        val snoozedUntil = userPreferences.supportPromptSnoozedUntil.first()
        val shouldShow = SupportGate.shouldShow(
            billingAvailable = billing.isAvailable,
            productsLoaded = billing.products.value.isNotEmpty(),
            alreadySupporter = billing.supporterTiers.value.isNotEmpty(),
            rootedCount = rootedCount,
            dismissCount = dismissCount,
            snoozedUntilEpochMs = snoozedUntil,
            nowEpochMs = System.currentTimeMillis(),
            reviewRequestedThisSession = reviewRequestedThisSession,
            updatePending = _uiState.value.updateStatus !is AppUpdateEvent.None,
        )
        if (BuildConfig.DEBUG) {
            Log.d(
                SUPPORT_GATE_TAG,
                "rootedCount=$rootedCount (need ${SupportGate.MIN_ROOTED_CHECKS}), " +
                    "dismissCount=$dismissCount (max ${SupportGate.MAX_DISMISSALS}), " +
                    "snoozedUntil=$snoozedUntil, reviewThisSession=$reviewRequestedThisSession " +
                    "-> shouldShow=$shouldShow",
            )
        }
        if (shouldShow) _uiState.update { it.copy(supportPromptVisible = true) }
    }

    /**
     * The support card actually reached the screen. Reported from the UI, not the gate, and at most
     * once per process.
     *
     * `MainScreen` reports from a `LaunchedEffect` keyed on the card's visibility, which re-runs
     * every time the card becomes visible again — on an activity recreation (rotation, fold/unfold,
     * resize), and when an update card that took the slot gives it back. Only one genuine offer can
     * happen per process, because answering the card either way snoozes it for a month, so a second
     * report would always be the same card counted twice.
     */
    fun onSupportPromptShown() {
        if (supportCardReported) return
        supportCardReported = true
        Analytics.trackSupportCardShown()
    }

    /**
     * "Support development" tapped. Opening the tip jar is an answer either way, so the card steps
     * aside for the snooze window — but unlike a dismissal it doesn't count against the cap.
     */
    fun onSupportPromptOpened() {
        Analytics.trackTipJarOpened(TIP_SOURCE_SUPPORT_CARD)
        _uiState.update { it.copy(supportPromptVisible = false) }
        viewModelScope.launch { snoozeSupportPrompt() }
    }

    fun onSupportPromptDismissed() {
        _uiState.update { it.copy(supportPromptVisible = false) }
        viewModelScope.launch {
            val dismissCount = userPreferences.incrementSupportPromptDismissCount()
            snoozeSupportPrompt()
            Analytics.trackSupportCardDismissed(dismissCount)
        }
    }

    private suspend fun snoozeSupportPrompt() {
        userPreferences.setSupportPromptSnoozedUntil(
            SupportGate.snoozeUntil(System.currentTimeMillis())
        )
    }

    fun onTipSelected(tier: TipTier) {
        Analytics.trackTipSelected(tier.name)
        billing.launchPurchase(tier)
    }

    /**
     * Debug-only: forces the support card on, bypassing [SupportGate]. Needed because a debug build
     * carries the `.debug` applicationId, so Play Billing never returns tip products for it and the
     * real gate can't open — the card would otherwise be unreviewable on-device. Only ever called
     * from the debug-gated overflow item. The tip dialog it opens will show its loading state, since
     * there genuinely are no products here.
     */
    fun demoSupportPrompt() {
        _uiState.update { it.copy(supportPromptVisible = true) }
    }

    private fun playResultHaptic(result: RootResult) = when (result) {
        is RootResult.Rooted -> haptics.playSuccess()
        RootResult.NotRooted, RootResult.Unknown -> haptics.playError()
        is RootResult.RootedNotGranted -> haptics.playNeutral()
    }

    /**
     * Debug-only: forces [result] through the same flow a real check uses (CHECKING state, haptic
     * ramp, ~1s delay, then result + outcome haptic) so the animations and haptics can be exercised
     * on-device without a matching root state. Only ever called from the debug-gated demo dialog.
     */
    fun checkRootDemo(result: RootResult) {
        viewModelScope.launch {
            val hapticsOn = userPreferences.hapticsEnabled.first()
            startRootCheck()
            if (hapticsOn) haptics.startCheckingRamp()
            delay(1000)
            applyResult(result)
            if (hapticsOn) playResultHaptic(result)
        }
    }

    private fun startRootCheck() {
        _uiState.update {
            it.copy(
                rootStatus = RootStatus.CHECKING,
                rootProvider = RootProvider.UNKNOWN,
                rootManager = null,
                rootProviderVersion = null,
            )
        }
    }

    private data class ResolvedRoot(
        val status: RootStatus,
        val provider: RootProvider,
        val manager: RootManager?,
        val version: String?,
    )

    private fun applyResult(result: RootResult) {
        val resolved = when (result) {
            is RootResult.Rooted ->
                ResolvedRoot(RootStatus.ROOTED, result.provider, result.manager, result.version)
            RootResult.NotRooted -> ResolvedRoot(RootStatus.NOT_ROOTED, RootProvider.UNKNOWN, null, null)
            RootResult.Unknown -> ResolvedRoot(RootStatus.UNKNOWN, RootProvider.UNKNOWN, null, null)
            is RootResult.RootedNotGranted ->
                ResolvedRoot(RootStatus.NOT_GRANTED, result.provider, result.manager, null)
        }
        _uiState.update {
            it.copy(
                rootStatus = resolved.status,
                rootProvider = resolved.provider,
                rootManager = resolved.manager,
                rootProviderVersion = resolved.version,
            )
        }
        Analytics.trackRootCheckResult(resolved.status.name)
        if (resolved.status == RootStatus.ROOTED) {
            Analytics.trackRootProvider(resolved.provider.name, resolved.manager?.name, resolved.version)
        }
    }

    fun onUpdateRequested() {
        if (demoUpdateActive) {
            demoUpdateDownloading()
            return
        }
        appUpdateController.startFlexibleFlow()
    }

    fun onInstallRequested() {
        if (demoUpdateActive) {
            // Can't actually restart into a fake update — just hide the card.
            demoUpdate(AppUpdateEvent.None)
            return
        }
        appUpdateController.completeUpdate()
    }

    // ---- Debug-only in-app-update demo ----
    // Pushes fake AppUpdateEvents straight into updateStatus, bypassing the Play controller, so the
    // UpdateCard's states and download animation can be exercised without a real Play update. Only
    // ever reached from the debug-gated demo dialog and the demo-aware buttons above.
    private var demoUpdateActive = false
    private var demoUpdateJob: Job? = null

    fun demoUpdate(event: AppUpdateEvent) {
        demoUpdateJob?.cancel()
        demoUpdateActive = event != AppUpdateEvent.None
        _uiState.update { it.copy(updateStatus = event) }
    }

    fun demoUpdateDownloading() {
        demoUpdateJob?.cancel()
        demoUpdateActive = true
        demoUpdateJob = viewModelScope.launch {
            val total = 18L * 1024 * 1024
            val step = total / 24
            var downloaded = 0L
            while (downloaded < total) {
                _uiState.update {
                    it.copy(updateStatus = AppUpdateEvent.Downloading(downloaded, total))
                }
                delay(120)
                downloaded += step
            }
            _uiState.update { it.copy(updateStatus = AppUpdateEvent.Downloaded) }
        }
    }

    override fun onCleared() {
        haptics.cancel()
    }

    private companion object {
        /**
         * Whether the Play review flow was requested anywhere in this process. Held per-process
         * rather than per-ViewModel so an activity recreation can't let the support card slip into
         * the same session as the review card. Mirrors the one-shot flag in `Analytics`.
         */
        @Volatile
        private var reviewRequestedThisSession = false

        /**
         * Whether `supportCardShown` has been reported in this process. Per-process for the same
         * reason as the flag above: activity recreation restarts the composition that reports it,
         * and the same card must not be counted twice. See [onSupportPromptShown].
         */
        @Volatile
        private var supportCardReported = false
    }
}
