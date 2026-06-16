package com.iboalali.basicrootchecker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

    companion object {
        private val TELEMETRY_ENABLED = booleanPreferencesKey("telemetry_enabled")
        private val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val LAST_SEEN_VERSION_CODE = intPreferencesKey("last_seen_version_code")
        private val PENDING_TIP_TOKENS = stringSetPreferencesKey("pending_tip_tokens")
    }
}
