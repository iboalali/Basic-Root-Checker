package com.iboalali.basicrootchecker.ui.about

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iboalali.appcatalog.ui.OtherApp
import com.iboalali.appcatalog.ui.toOtherApp
import com.iboalali.basicrootchecker.BasicRootCheckerApplication
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Backs the About screen's "Other apps" card. **Read-only:** it only observes the app-scoped
 * [com.iboalali.appcatalog.data.AppCatalogRepository] (the shared one from
 * `com.iboalali.appcatalog:data`); the catalog fetch is owned by `MainActivity` (kicked off once at
 * app start). This VM just projects the cached/bundled list for the UI.
 *
 * The row model is the shared [OtherApp] rather than one of this app's own — it is `@Immutable`, so
 * the card's rows stay skippable, and it drops the `changelog` field the About screen doesn't show.
 *
 * This app is already excluded from the list by the repository, which derives the running package
 * itself — that filtering used to live here, and identically in the other two apps.
 */
class AboutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as BasicRootCheckerApplication).appCatalogRepository

    val otherApps: StateFlow<ImmutableList<OtherApp>> =
        repository.otherApps
            .map { apps -> apps.map { it.toOtherApp() }.toImmutableList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())
}
