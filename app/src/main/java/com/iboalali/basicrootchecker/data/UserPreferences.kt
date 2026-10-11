package com.iboalali.basicrootchecker.data

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.iboalali.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userSettingsDataStore by preferencesDataStore(name = "user_settings")

/**
 * The last root check, in its own file so backup can leave it out (see `backup_rules.xml` and
 * `data_extraction_rules.xml`). It describes one device, and restored onto another it would report
 * the old device's result until the first check. Installs that kept it in [userSettingsDataStore]
 * move it here on first read.
 */
private val Context.rootCheckDataStore by
    preferencesDataStore(
        name = "root_check",
        produceMigrations = { context -> listOf(LastRootCheckMigration(context)) },
    )

private class LastRootCheckMigration(private val context: Context) : DataMigration<Preferences> {

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        UserPreferences.LAST_ROOT_CHECK_KEYS.any { it in context.userSettingsDataStore.data.first() }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val legacy = context.userSettingsDataStore.data.first()
        val migrated = currentData.toMutablePreferences()
        UserPreferences.LAST_ROOT_CHECK_KEYS.forEach { key -> migrated.copyFrom(legacy, key) }
        return migrated.toPreferences()
    }

    override suspend fun cleanUp() {
        context.userSettingsDataStore.edit { preferences ->
            UserPreferences.LAST_ROOT_CHECK_KEYS.forEach { preferences.remove(it) }
        }
    }

    private fun <T> MutablePreferences.copyFrom(source: Preferences, key: Preferences.Key<T>) {
        source[key]?.let { this[key] = it }
    }
}

class UserPreferences(private val context: Context) {

    val telemetryEnabled: Flow<Boolean> =
        context.userSettingsDataStore.data.map { preferences ->
            preferences[TELEMETRY_ENABLED] ?: true
        }

    suspend fun setTelemetryEnabled(enabled: Boolean) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[TELEMETRY_ENABLED] = enabled
        }
    }

    val hapticsEnabled: Flow<Boolean> =
        context.userSettingsDataStore.data.map { preferences ->
            preferences[HAPTICS_ENABLED] ?: true
        }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[HAPTICS_ENABLED] = enabled
        }
    }

    val themeMode: Flow<ThemeMode> =
        context.userSettingsDataStore.data.map { preferences ->
            ThemeMode.fromStorage(preferences[THEME_MODE])
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }

    val lastSeenVersionCode: Flow<Int> =
        context.userSettingsDataStore.data.map { preferences ->
            preferences[LAST_SEEN_VERSION_CODE] ?: 0
        }

    suspend fun setLastSeenVersionCode(code: Int) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[LAST_SEEN_VERSION_CODE] = code
        }
    }

    /**
     * Purchase tokens of tips seen in the PENDING state. A token is added when a tip is reported
     * pending and removed once it clears, so the billing layer can tell a genuine late clear (token
     * present) from the routine re-grant of an already-owned tip on every connect (token absent).
     * Survives process death so a clear that happens while the app is closed is still recognized on
     * the next launch.
     */
    suspend fun addPendingTipToken(token: String) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[PENDING_TIP_TOKENS] =
                (preferences[PENDING_TIP_TOKENS] ?: emptySet()) + token
        }
    }

    /**
     * Removes [token] in a single DataStore transaction and returns whether this call removed it.
     * Concurrent callers for the same token are serialized, so exactly one of them gets `true`.
     */
    suspend fun removePendingTipToken(token: String): Boolean {
        var removed = false
        context.userSettingsDataStore.edit { preferences ->
            val tokens = preferences[PENDING_TIP_TOKENS] ?: emptySet()
            removed = token in tokens
            if (removed) preferences[PENDING_TIP_TOKENS] = tokens - token
        }
        return removed
    }

    /**
     * The most recent root check, or null if none has run yet. Recorded by [RootChecker] on every
     * check (UI or AppFunction), so AppFunctions can report the last result and its time without
     * re-probing.
     */
    val lastRootCheck: Flow<LastRootCheck?> =
        context.rootCheckDataStore.data.map { preferences ->
            val checkedAt = preferences[LAST_ROOT_CHECK_AT] ?: return@map null
            LastRootCheck(
                checkedAtEpochMs = checkedAt,
                status =
                    preferences[LAST_ROOT_CHECK_STATUS]?.let {
                        runCatching { RootCheckStatus.valueOf(it) }.getOrNull()
                    } ?: RootCheckStatus.UNKNOWN,
                provider =
                    preferences[LAST_ROOT_CHECK_PROVIDER]?.let {
                        runCatching { RootProvider.valueOf(it) }.getOrNull()
                    },
                manager =
                    preferences[LAST_ROOT_CHECK_MANAGER]?.let {
                        runCatching { RootManager.valueOf(it) }.getOrNull()
                    },
                version = preferences[LAST_ROOT_CHECK_VERSION],
            )
        }

    suspend fun recordRootCheck(check: LastRootCheck) {
        context.rootCheckDataStore.edit { preferences ->
            preferences[LAST_ROOT_CHECK_AT] = check.checkedAtEpochMs
            preferences[LAST_ROOT_CHECK_STATUS] = check.status.name
            // Remove rather than leave stale values when a field is absent (e.g. a later
            // NotRooted result must not retain the provider/version of a prior Rooted one).
            check.provider?.let { preferences[LAST_ROOT_CHECK_PROVIDER] = it.name }
                ?: preferences.remove(LAST_ROOT_CHECK_PROVIDER)
            check.manager?.let { preferences[LAST_ROOT_CHECK_MANAGER] = it.name }
                ?: preferences.remove(LAST_ROOT_CHECK_MANAGER)
            check.version?.let { preferences[LAST_ROOT_CHECK_VERSION] = it }
                ?: preferences.remove(LAST_ROOT_CHECK_VERSION)
        }
    }

    /**
     * Running count of root checks run from the main screen, whatever they found, used to gate the
     * support card. Sibling of [rootedCheckCount], which counts only the root-found ones: the two
     * post-check asks weigh different things, so they count different things. Checks made through
     * an AppFunction are left out, because the card they gate lives on a screen the caller isn't
     * looking at.
     *
     * An install that predates this key seeds it from [rootedCheckCount], so progress already earned
     * toward the card doesn't restart at zero. The seed undercounts real history (it knows nothing
     * about checks that found no root), which is the safe direction: it can only delay the ask.
     */
    val checkCount: Flow<Int> =
        context.userSettingsDataStore.data.map { preferences -> preferences.checkCountOrSeed() }

    suspend fun incrementCheckCount(): Int {
        var newCount = 0
        context.userSettingsDataStore.edit { preferences ->
            newCount = preferences.checkCountOrSeed() + 1
            preferences[CHECK_COUNT] = newCount
        }
        return newCount
    }

    /**
     * [CHECK_COUNT], or the [ROOTED_CHECK_COUNT] it seeds from on an install that has never written
     * it. Read inside the same transaction as the write above, so the carry-over happens exactly
     * once: the first increment stores the key and the fallback is never reached again. The two
     * counters diverge from that point on, which is the intent.
     */
    private fun Preferences.checkCountOrSeed(): Int =
        this[CHECK_COUNT] ?: this[ROOTED_CHECK_COUNT] ?: 0

    /** Running count of checks that found root, used to gate the in-app review prompt. */
    val rootedCheckCount: Flow<Int> =
        context.userSettingsDataStore.data.map { preferences ->
            preferences[ROOTED_CHECK_COUNT] ?: 0
        }

    suspend fun incrementRootedCheckCount(): Int {
        var newCount = 0
        context.userSettingsDataStore.edit { preferences ->
            newCount = (preferences[ROOTED_CHECK_COUNT] ?: 0) + 1
            preferences[ROOTED_CHECK_COUNT] = newCount
        }
        return newCount
    }

    /**
     * Version code at which the in-app review prompt last fired (0 if never), to cap it to once per
     * version.
     */
    val lastReviewPromptVersionCode: Flow<Int> =
        context.userSettingsDataStore.data.map { preferences ->
            preferences[LAST_REVIEW_PROMPT_VERSION_CODE] ?: 0
        }

    suspend fun setLastReviewPromptVersionCode(code: Int) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[LAST_REVIEW_PROMPT_VERSION_CODE] = code
        }
    }

    /**
     * When the main screen's support card becomes eligible again (0 if never snoozed). Set whenever
     * the user answers it (by dismissing *or* by opening the tip jar), so a look at the prices
     * isn't followed by another ask on the next check. See `SupportGate`.
     */
    val supportPromptSnoozedUntil: Flow<Long> =
        context.userSettingsDataStore.data.map { preferences ->
            preferences[SUPPORT_PROMPT_SNOOZED_UNTIL] ?: 0L
        }

    suspend fun setSupportPromptSnoozedUntil(epochMs: Long) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[SUPPORT_PROMPT_SNOOZED_UNTIL] = epochMs
        }
    }

    /** How many times the support card has been dismissed; past the cap it never returns. */
    val supportPromptDismissCount: Flow<Int> =
        context.userSettingsDataStore.data.map { preferences ->
            preferences[SUPPORT_PROMPT_DISMISS_COUNT] ?: 0
        }

    suspend fun incrementSupportPromptDismissCount(): Int {
        var newCount = 0
        context.userSettingsDataStore.edit { preferences ->
            newCount = (preferences[SUPPORT_PROMPT_DISMISS_COUNT] ?: 0) + 1
            preferences[SUPPORT_PROMPT_DISMISS_COUNT] = newCount
        }
        return newCount
    }

    companion object {
        private val TELEMETRY_ENABLED = booleanPreferencesKey("telemetry_enabled")
        private val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val LAST_SEEN_VERSION_CODE = intPreferencesKey("last_seen_version_code")
        private val PENDING_TIP_TOKENS = stringSetPreferencesKey("pending_tip_tokens")
        private val LAST_ROOT_CHECK_AT = longPreferencesKey("last_root_check_at")
        private val LAST_ROOT_CHECK_STATUS = stringPreferencesKey("last_root_check_status")
        private val LAST_ROOT_CHECK_PROVIDER = stringPreferencesKey("last_root_check_provider")
        private val LAST_ROOT_CHECK_MANAGER = stringPreferencesKey("last_root_check_manager")
        private val LAST_ROOT_CHECK_VERSION = stringPreferencesKey("last_root_check_version")
        internal val LAST_ROOT_CHECK_KEYS: List<Preferences.Key<*>> =
            listOf(
                LAST_ROOT_CHECK_AT,
                LAST_ROOT_CHECK_STATUS,
                LAST_ROOT_CHECK_PROVIDER,
                LAST_ROOT_CHECK_MANAGER,
                LAST_ROOT_CHECK_VERSION,
            )
        private val CHECK_COUNT = intPreferencesKey("check_count")
        private val ROOTED_CHECK_COUNT = intPreferencesKey("rooted_check_count")
        private val LAST_REVIEW_PROMPT_VERSION_CODE =
            intPreferencesKey("last_review_prompt_version_code")
        private val SUPPORT_PROMPT_SNOOZED_UNTIL =
            longPreferencesKey("support_prompt_snoozed_until")
        private val SUPPORT_PROMPT_DISMISS_COUNT = intPreferencesKey("support_prompt_dismiss_count")
    }
}
