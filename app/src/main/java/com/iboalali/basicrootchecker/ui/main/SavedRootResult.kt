package com.iboalali.basicrootchecker.ui.main

import androidx.lifecycle.SavedStateHandle
import com.iboalali.basicrootchecker.data.RootManager
import com.iboalali.basicrootchecker.data.RootProvider

/**
 * Keeps the shown root result in saved instance state, so the main screen still shows it after the
 * system kills and restores the process. Saved state stays on this device, unlike the backed-up
 * `last_root_check_*` preferences, so a result restored from another phone can never appear here.
 */
internal object SavedRootResult {
    private const val KEY_STATUS = "root_status"
    private const val KEY_PROVIDER = "root_provider"
    private const val KEY_MANAGER = "root_manager"
    private const val KEY_VERSION = "root_provider_version"

    fun save(handle: SavedStateHandle, state: MainUiState) {
        handle[KEY_STATUS] = state.rootStatus.name
        handle[KEY_PROVIDER] = state.rootProvider.name
        handle[KEY_MANAGER] = state.rootManager?.name
        handle[KEY_VERSION] = state.rootProviderVersion
    }

    /** [state] with the saved result applied, or unchanged when no result was saved. */
    fun restore(handle: SavedStateHandle, state: MainUiState): MainUiState {
        val status =
            handle.get<String>(KEY_STATUS)?.let { name ->
                RootStatus.entries.firstOrNull { it.name == name }
            }
        if (status == null || status == RootStatus.NOT_CHECKED || status == RootStatus.CHECKING) {
            return state
        }
        val providerName = handle.get<String>(KEY_PROVIDER)
        val managerName = handle.get<String>(KEY_MANAGER)
        return state.copy(
            rootStatus = status,
            rootProvider =
                RootProvider.entries.firstOrNull { it.name == providerName } ?: RootProvider.UNKNOWN,
            rootManager = RootManager.entries.firstOrNull { it.name == managerName },
            rootProviderVersion = handle.get<String>(KEY_VERSION),
        )
    }
}
