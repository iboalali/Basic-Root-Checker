package com.iboalali.basicrootchecker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

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

    fun telemetryEnabledBlocking(): Boolean =
        runBlocking { telemetryEnabled.first() }

    val lastSeenVersionCode: Flow<Int> =
        context.userSettingsDataStore.data.map { preferences ->
            preferences[LAST_SEEN_VERSION_CODE] ?: 0
        }

    suspend fun setLastSeenVersionCode(code: Int) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[LAST_SEEN_VERSION_CODE] = code
        }
    }

    companion object {
        private val TELEMETRY_ENABLED = booleanPreferencesKey("telemetry_enabled")
        private val LAST_SEEN_VERSION_CODE = intPreferencesKey("last_seen_version_code")
    }
}
