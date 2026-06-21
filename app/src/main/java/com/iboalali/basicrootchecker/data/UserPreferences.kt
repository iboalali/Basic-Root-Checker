package com.iboalali.basicrootchecker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userSettingsDataStore by preferencesDataStore(name = "user_settings")

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
     * Purchase tokens of tips seen in the PENDING state. A token is added when a tip is
     * reported pending and removed once it clears, so the billing layer can tell a genuine
     * late clear (token present) from the routine re-grant of an already-owned tip on every
     * connect (token absent). Survives process death so a clear that happens while the app
     * is closed is still recognized on the next launch.
     */
    val pendingTipTokens: Flow<Set<String>> =
        context.userSettingsDataStore.data.map { preferences ->
            preferences[PENDING_TIP_TOKENS] ?: emptySet()
        }

    suspend fun addPendingTipToken(token: String) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[PENDING_TIP_TOKENS] = (preferences[PENDING_TIP_TOKENS] ?: emptySet()) + token
        }
    }

    suspend fun clearPendingTipTokens(tokens: Set<String>) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[PENDING_TIP_TOKENS] = (preferences[PENDING_TIP_TOKENS] ?: emptySet()) - tokens
        }
    }

    /**
     * The most recent root check, or null if none has run yet. Recorded by [RootChecker] on
     * every check (UI or AppFunction), so AppFunctions can report the last result and its time
     * without re-probing.
     */
    val lastRootCheck: Flow<LastRootCheck?> =
        context.userSettingsDataStore.data.map { preferences ->
            val checkedAt = preferences[LAST_ROOT_CHECK_AT] ?: return@map null
            LastRootCheck(
                checkedAtEpochMs = checkedAt,
                status = preferences[LAST_ROOT_CHECK_STATUS]
                    ?.let { runCatching { RootCheckStatus.valueOf(it) }.getOrNull() }
                    ?: RootCheckStatus.UNKNOWN,
                provider = preferences[LAST_ROOT_CHECK_PROVIDER]
                    ?.let { runCatching { RootProvider.valueOf(it) }.getOrNull() },
                manager = preferences[LAST_ROOT_CHECK_MANAGER]
                    ?.let { runCatching { RootManager.valueOf(it) }.getOrNull() },
                version = preferences[LAST_ROOT_CHECK_VERSION],
            )
        }

    suspend fun recordRootCheck(check: LastRootCheck) {
        context.userSettingsDataStore.edit { preferences ->
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
    }
}
