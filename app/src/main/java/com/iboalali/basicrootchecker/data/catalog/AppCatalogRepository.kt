package com.iboalali.basicrootchecker.data.catalog

import android.content.Context
import android.util.Log
import com.iboalali.basicrootchecker.analytics.Analytics
import com.iboalali.basicrootchecker.analytics.CATALOG_REFRESH_FAILURE
import com.iboalali.basicrootchecker.analytics.CATALOG_REFRESH_NOT_MODIFIED
import com.iboalali.basicrootchecker.analytics.CATALOG_REFRESH_UPDATED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale

/**
 * Source of truth for the "Other apps" catalog.
 *
 * The list is shown from the best data available, in priority order:
 *  1. the latest successful download from the website,
 *  2. the cached copy of a previous download (offline),
 *  3. the per-locale snapshot bundled in `assets/apps.<locale>.json`, with English `apps.json` as
 *     the final fallback (first run, offline).
 *
 * **Localization.** The feed is published one file per locale — `apps.json` (English default),
 * `apps.de.json`, `apps.ar.json`, `apps.es.json`, `apps.ru.json`. We request the file for the
 * device language and, per the feed contract, fall back to English when the localized file is
 * absent (a non-2xx primary response, e.g. `404`). The catalog locales are independent of the app's
 * own UI languages. Each locale is cached separately (`apps_catalog_<key>.json`), so switching
 * language shows that language's cached list right away and offline use is correct per language.
 *
 * [refresh] revalidates the catalog in the background using a conditional GET, delegated to
 * [CatalogHttpSource] / OkHttp's cache: the stored `ETag`/`Last-Modified` go back as
 * `If-None-Match`/`If-Modified-Since`, so the server answers `304 Not Modified` (no body) when
 * nothing changed and we only download a payload that actually changed. On a change it caches the new
 * JSON and updates [apps]; on `304`, or on any failure (no/slow connection, parse error), it leaves
 * the current list in place. Nothing here blocks the UI — consumers observe [apps] and recompose if a
 * fresher list arrives. The fetch is kicked off once at app start from `MainActivity`; the About
 * screen only observes [apps] and never triggers it.
 *
 * **Two caches, on purpose.** OkHttp's (in `cacheDir`) is the HTTP layer's — it exists so
 * revalidation works. Ours (`apps_catalog_<key>.json` in `filesDir`) is the last known good payload,
 * read directly at startup to seed [apps] with no network stack involved, and it must survive the
 * system reclaiming `cacheDir`.
 */
class AppCatalogRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val _apps = MutableStateFlow<List<CatalogApp>>(emptyList())
    val apps: StateFlow<List<CatalogApp>> = _apps.asStateFlow()

    // The catalog key (mapped device language) the in-memory list was last seeded/fetched for. The
    // singleton outlives the activity, so a runtime language switch (Android 13+ recreates the
    // activity and calls refresh() again) can re-seed from the new locale's cache before the network
    // returns. @Volatile: written/read across IO-dispatcher threads.
    @Volatile
    private var loadedKey: String? = null

    // Serializes publishing to [_apps] so a "don't downgrade" check and the write it guards can't be
    // interleaved. Only ever held around in-memory work — never across the fetch.
    private val stateMutex = Mutex()

    // The catalog key a *network* response has been published for, or null. Guarded by [stateMutex],
    // so it needs no @Volatile.
    private var networkAppliedKey: String? = null

    // Lazy so constructing the repository (which happens on first access from the Application) costs
    // nothing until a refresh actually runs.
    private val http by lazy { CatalogHttpSource(File(context.cacheDir, HTTP_CACHE_DIR)) }

    init {
        // Seed off the main thread from the cache (or the bundled snapshot) so the screen has data
        // before — and regardless of whether — the network refresh completes.
        scope.launch {
            seedFromCache(currentCatalogKey())
            deleteLegacyValidatorFiles()
        }
    }

    /**
     * Publishes the cached (or bundled) list for [key] — unless a network response for that same
     * locale has already been published.
     *
     * That guard matters because this and [refresh] both run on the multi-threaded IO dispatcher: the
     * startup seed's file read can finish *after* a fetch that already delivered fresher data, and
     * without the check it would quietly replace the new list with the older cached one. A seed for a
     * *different* locale (a language switch) still wins, which is the point — the cache for the newly
     * selected language is more correct than a network list for the previous one.
     */
    private suspend fun seedFromCache(key: String) {
        // Read outside the lock: this is blocking file IO and holding the mutex across it would
        // serialize the seed against the fetch's result being published.
        val loaded = loadCachedOrBundled(key)
        stateMutex.withLock {
            if (networkAppliedKey == key) return
            _apps.value = loaded
            loadedKey = key
        }
    }

    /** Publishes a freshly fetched list for [key] and records that the network has spoken for it. */
    private suspend fun applyFetched(key: String, fresh: List<CatalogApp>) {
        stateMutex.withLock {
            _apps.value = fresh
            loadedKey = key
            networkAppliedKey = key
        }
    }

    private fun loadCachedOrBundled(key: String): List<CatalogApp> {
        val cache = cacheFile(key)
        runCatching { if (cache.exists()) return parse(cache.readText()) }
            .onFailure { Log.w(TAG, "Failed to read cached app catalog ($key)", it) }
        // Bundled offline fallback (first run / offline): prefer this locale's snapshot, then English
        // (`apps.json`). A localized snapshot may be absent for a key, so a failure there falls
        // through to English.
        val localized = assetFileForKey(key)
        return runCatching { parse(readAsset(localized)) }
            .recoverCatching { if (localized != ASSET_FILE) parse(readAsset(ASSET_FILE)) else throw it }
            .onFailure { Log.w(TAG, "Failed to read bundled app catalog ($key)", it) }
            .getOrDefault(emptyList())
    }

    private fun readAsset(name: String): String =
        context.assets.open(name).bufferedReader().use { it.readText() }

    /** Bundled asset for [key]: the locale's snapshot when published, else the English default. */
    private fun assetFileForKey(key: String): String =
        if (key in LOCALIZED_LOCALES) "apps.$key.json" else ASSET_FILE

    private fun parse(text: String): List<CatalogApp> = json.decodeFromString<AppCatalog>(text).apps

    /**
     * Revalidates the catalog for the current device language; applies and caches it only if the
     * server reports a change. Safe to call anytime. Reports the outcome (updated / not-modified /
     * failure) to analytics.
     */
    fun refresh() {
        scope.launch {
            val key = currentCatalogKey()
            // Language changed since the last seed (the singleton outlives the recreated activity):
            // switch the displayed list to the new locale's cache/bundled copy right away, so the UI
            // doesn't keep showing the previous language until the network returns. Reading loadedKey
            // unlocked is fine — a stale read only costs a redundant seed, which seedFromCache's
            // don't-downgrade check absorbs.
            if (key != loadedKey) seedFromCache(key)
            runCatching {
                when (val result = fetch(key)) {
                    FetchResult.NotModified -> CATALOG_REFRESH_NOT_MODIFIED
                    is FetchResult.Updated -> {
                        val fresh = parse(result.body)
                        require(fresh.isNotEmpty()) { "Catalog contained no apps" }
                        applyFetched(key, fresh)
                        // Best-effort persistence; must not turn a good fetch into a failure. Only
                        // the payload is ours to keep now — the validators live in OkHttp's cache.
                        runCatching { cacheFile(key).writeText(result.body) }
                            .onFailure { Log.w(TAG, "Failed to cache app catalog ($key)", it) }
                        CATALOG_REFRESH_UPDATED
                    }
                }
            }.onSuccess { result ->
                Analytics.trackAppCatalogRefresh(result)
            }.onFailure { e ->
                Log.i(TAG, "App catalog refresh failed; keeping current list", e)
                Analytics.trackAppCatalogRefresh(CATALOG_REFRESH_FAILURE, error = e.javaClass.simpleName)
            }
        }
    }

    /**
     * Conditional GET for [key]'s locale file, falling back to English on a non-2xx primary response
     * (the localized file may not exist — the feed contract requires clients to fall back to
     * `/apps.json`). Throws on a transport failure or when no source is reachable.
     *
     * OkHttp's cache stores and replays the validators per URL, so the localized file and the English
     * fallback can never be confused for one another.
     */
    private fun fetch(key: String): FetchResult {
        val primaryUrl = urlForKey(key)
        http.fetch(primaryUrl)?.let { return it.toFetchResult(key) }
        // Primary unavailable (e.g. 404 for an untranslated locale): fall back to English.
        if (primaryUrl != ENGLISH_URL) {
            http.fetch(ENGLISH_URL)?.let { return it.toFetchResult(key) }
        }
        error("Catalog unavailable")
    }

    /**
     * Nothing transferred means the catalog we already hold is current, so keep the list as-is. The
     * exception is having lost our own snapshot (the HTTP cache in `cacheDir` and the snapshot in
     * `filesDir` are evicted independently): then take the body OkHttp handed back, so the next
     * launch seeds from a real catalog instead of the bundled asset.
     */
    private fun CatalogPayload.toFetchResult(key: String): FetchResult =
        if (!transferred && cacheFile(key).exists()) FetchResult.NotModified
        else FetchResult.Updated(json)

    /** Catalog cache key for the current device language: the locale code if the feed publishes one, else English. */
    private fun currentCatalogKey(): String {
        val language = Locale.getDefault().language.lowercase(Locale.ROOT)
        return if (language in LOCALIZED_LOCALES) language else ENGLISH_KEY
    }

    private fun urlForKey(key: String): String =
        if (key in LOCALIZED_LOCALES) "$BASE_URL.$key.json" else ENGLISH_URL

    private fun cacheFile(key: String) = File(context.filesDir, "apps_catalog_$key.json")

    /**
     * Removes the `apps_catalog_<key>.validators` sidecar files the pre-OkHttp implementation wrote;
     * the ETag/Last-Modified they held is OkHttp's cache's business now. Best-effort — a leftover file
     * is inert, just dead weight on disk. Called at every startup, but after the first upgrade that's
     * five `delete()` calls on absent files, which is cheaper than tracking whether it already ran.
     */
    private fun deleteLegacyValidatorFiles() {
        (LOCALIZED_LOCALES + ENGLISH_KEY).forEach { key ->
            runCatching { File(context.filesDir, "apps_catalog_$key.validators").delete() }
        }
    }

    private sealed interface FetchResult {
        /** A catalog to apply: either freshly downloaded, or recovered after losing our snapshot. */
        data class Updated(val body: String) : FetchResult

        /** Nothing was transferred — the catalog we already hold is current. */
        data object NotModified : FetchResult
    }

    companion object {
        private const val TAG = "AppCatalogRepository"
        private const val BASE_URL = "https://iboalali.com/apps"
        private const val ENGLISH_URL = "$BASE_URL.json"
        private const val ENGLISH_KEY = "en"

        /**
         * Locales the feed publishes a translated `apps.<locale>.json` file for. English is the
         * default/bare file, so it's not listed. Mirror this with the feed's locale list; an unknown
         * device language falls back to English (and the mandatory 404 fallback covers any drift).
         */
        private val LOCALIZED_LOCALES = setOf("de", "ar", "es", "ru")

        private const val ASSET_FILE = "apps.json"

        /**
         * OkHttp cache directory, under `cacheDir` so the system may reclaim it — losing it only
         * costs one full re-download, since the list itself is seeded from `filesDir`.
         */
        private const val HTTP_CACHE_DIR = "app_catalog_http"
    }
}
