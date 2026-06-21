package com.iboalali.basicrootchecker.data.catalog

import kotlinx.serialization.Serializable

/**
 * The app catalog published at the website and bundled as the offline default (`assets/apps.json`).
 * The feed is localized one file per locale — `apps.json` (English default), `apps.de.json`,
 * `apps.ar.json`, etc.; clients request the device-language file and fall back to English on a 404
 * (see [AppCatalogRepository]). Field names match the JSON; everything except the apps list is
 * optional so older/newer payloads still parse (paired with a lenient [kotlinx.serialization.json.Json]).
 */
@Serializable
data class AppCatalog(
    /**
     * The locale this payload was actually served as (`"en"`, `"de"`, `"ar"`, …). Reflects the file
     * fetched, not necessarily the language of every string (a field may be an English fallback).
     */
    val locale: String = "en",
    val apps: List<CatalogApp> = emptyList(),
)

@Serializable
data class CatalogApp(
    val name: String = "",
    val packageName: String? = null,
    val description: String = "",
    val icon: String? = null,
    val website: String? = null,
    val whatsNew: List<String> = emptyList(),
    val changelog: List<ChangelogEntry> = emptyList(),
)

@Serializable
data class ChangelogEntry(
    val version: String = "",
    val changes: List<String> = emptyList(),
)
