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
 * [com.iboalali.basicrootchecker.data.catalog.AppCatalogRepository]; the catalog fetch is owned by
 * `MainActivity` (kicked off once at app start). This VM just projects the cached/bundled list for
 * the UI and filters this app out of its own list.
 */
class AboutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as BasicRootCheckerApplication).appCatalogRepository

    // Strip the debug suffix so this app is filtered out of its own "Other apps" list in every build
    // variant (the catalog lists the release applicationId).
    private val selfPackage = application.packageName.removeSuffix(".debug")

    val otherApps: StateFlow<ImmutableList<OtherAppUi>> = repository.apps
        .map { apps ->
            apps.asSequence()
                .filter { it.packageName != selfPackage }
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
