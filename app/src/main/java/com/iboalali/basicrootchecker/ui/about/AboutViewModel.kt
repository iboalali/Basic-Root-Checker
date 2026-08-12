package com.iboalali.basicrootchecker.ui.about

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iboalali.basicrootchecker.BasicRootCheckerApplication
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** A single entry in the About → "Other apps" list, projected from the catalog for the UI. */
data class OtherAppUi(
    val name: String,
    val description: String,
    val iconUrl: String?,
    val website: String?,
    val packageName: String?,
    /** Latest highlights (localized, may be empty); bullets can contain inline Markdown. */
    val highlights: ImmutableList<String>,
)

/**
 * Backs the About screen's "Other apps" card. **Read-only:** it only observes the app-scoped
 * [com.iboalali.appcatalog.data.AppCatalogRepository] (the shared one from
 * `com.iboalali.appcatalog:data`); the catalog fetch is owned by `MainActivity` (kicked off once at
 * app start). This VM just projects the cached/bundled list for the UI.
 *
 * This app is already excluded from the list by the repository, which derives the running package
 * itself — that filtering used to live here, and identically in the other two apps.
 */
class AboutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as BasicRootCheckerApplication).appCatalogRepository

    val otherApps: StateFlow<ImmutableList<OtherAppUi>> =
        repository.otherApps
            .map { apps ->
                apps
                    .asSequence()
                    .map {
                        OtherAppUi(
                            name = it.name,
                            description = it.description,
                            iconUrl = it.icon,
                            website = it.website,
                            packageName = it.packageName,
                            highlights = it.highlights.toImmutableList(),
                        )
                    }
                    .toList()
                    .toImmutableList()
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())
}
