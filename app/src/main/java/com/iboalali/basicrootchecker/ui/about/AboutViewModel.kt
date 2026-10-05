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
 * [com.iboalali.appcatalog.data.AppCatalogRepository]. The catalog fetch is owned by
 * `MainActivity`, which starts it once at app start.
 *
 * The row model is the shared [OtherApp]. It is `@Immutable`, so the card's rows stay skippable,
 * and it drops the `changelog` field the About screen doesn't show.
 *
 * The repository already excludes this app from the list, because it derives the running package
 * itself.
 */
class AboutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as BasicRootCheckerApplication).appCatalogRepository

    val otherApps: StateFlow<ImmutableList<OtherApp>> =
        repository.otherApps
            .map { apps -> apps.map { it.toOtherApp() }.toImmutableList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())
}
