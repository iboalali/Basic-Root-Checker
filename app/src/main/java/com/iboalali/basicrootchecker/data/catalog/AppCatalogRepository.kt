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
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.zip.GZIPInputStream

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
 * [refresh] revalidates the catalog in the background using a conditional GET: it sends the stored
 * `ETag`/`Last-Modified` from the previous download as `If-None-Match`/`If-Modified-Since`, so the
 * server answers `304 Not Modified` (no body) when nothing changed — we only download the payload
 * when it actually changed. On a change it caches the new JSON (and validators) and updates [apps];
 * on `304`, or on any failure (no/slow connection, parse error), it leaves the current list in
 * place. Nothing here blocks the UI — consumers observe [apps] and recompose if a fresher list
 * arrives. The fetch is kicked off once at app start from `MainActivity`; the About screen only
 * observes [apps] and never triggers it.
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

    init {
        // Seed off the main thread from the cache (or the bundled snapshot) so the screen has data
        // before — and regardless of whether — the network refresh completes.
        scope.launch {
            seedFromCache(currentCatalogKey())
        }
    }

    private fun seedFromCache(key: String) {
        _apps.value = loadCachedOrBundled(key)
        loadedKey = key
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
            // doesn't keep showing the previous language until the network returns.
            if (key != loadedKey) seedFromCache(key)
            runCatching {
                when (val result = fetch(key)) {
                    FetchResult.NotModified -> CATALOG_REFRESH_NOT_MODIFIED
                    is FetchResult.Updated -> {
                        val fresh = parse(result.body)
                        require(fresh.isNotEmpty()) { "Catalog contained no apps" }
                        _apps.value = fresh
                        loadedKey = key
                        // Best-effort persistence; must not turn a good fetch into a failure.
                        runCatching { cacheFile(key).writeText(result.body) }
                            .onFailure { Log.w(TAG, "Failed to cache app catalog ($key)", it) }
                        saveValidators(key, result.url, result.etag, result.lastModified)
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
     */
    private fun fetch(key: String): FetchResult {
        val primaryUrl = urlForKey(key)
        // Validators only matter when we still hold the body they validate; otherwise a 304 would
        // leave us with nothing to show. They carry the URL they were captured from so we never
        // replay them against a different file (e.g. the English fallback vs. the localized file).
        val validators = if (cacheFile(key).exists()) loadValidators(key) else Validators(null, null, null)

        requestCatalog(primaryUrl, validators)?.let { return it }
        // Primary unavailable (e.g. 404 for an untranslated locale): fall back to English.
        if (primaryUrl != ENGLISH_URL) {
            requestCatalog(ENGLISH_URL, validators)?.let { return it }
        }
        error("Catalog unavailable")
    }

    /**
     * Performs one conditional GET. Returns the [FetchResult] for `200`/`304`, or `null` for a
     * response we should fall back on (any other status, e.g. `404`). [validators] are sent only
     * when they were captured from this exact [url], so a `304` always has a matching body to keep.
     */
    private fun requestCatalog(url: String, validators: Validators): FetchResult? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
            // Ask for a gzip-compressed transfer; we decompress below based on the response header.
            setRequestProperty("Accept-Encoding", "gzip")
            if (validators.url == url) {
                validators.etag?.let { setRequestProperty("If-None-Match", it) }
                validators.lastModified?.let { setRequestProperty("If-Modified-Since", it) }
            }
        }
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_NOT_MODIFIED -> return FetchResult.NotModified
                HttpURLConnection.HTTP_OK -> Unit
                else -> return null
            }
            val stream = if (connection.contentEncoding.equals("gzip", ignoreCase = true)) {
                GZIPInputStream(connection.inputStream)
            } else {
                connection.inputStream
            }
            val body = stream.bufferedReader().use { it.readText() }
            return FetchResult.Updated(
                url = url,
                body = body,
                etag = connection.getHeaderField("ETag"),
                lastModified = connection.getHeaderField("Last-Modified"),
            )
        } finally {
            connection.disconnect()
        }
    }

    /** Catalog cache key for the current device language: the locale code if the feed publishes one, else English. */
    private fun currentCatalogKey(): String {
        val language = Locale.getDefault().language.lowercase(Locale.ROOT)
        return if (language in LOCALIZED_LOCALES) language else ENGLISH_KEY
    }

    private fun urlForKey(key: String): String =
        if (key in LOCALIZED_LOCALES) "$BASE_URL.$key.json" else ENGLISH_URL

    private fun cacheFile(key: String) = File(context.filesDir, "apps_catalog_$key.json")

    private fun validatorsFile(key: String) = File(context.filesDir, "apps_catalog_$key.validators")

    /** The cache validators from the last 200 response: source URL, ETag, then Last-Modified (one per line). */
    private fun loadValidators(key: String): Validators {
        val lines = runCatching {
            validatorsFile(key).let { if (it.exists()) it.readText().lines() else emptyList() }
        }.getOrDefault(emptyList())
        return Validators(
            url = lines.getOrNull(0)?.ifBlank { null },
            etag = lines.getOrNull(1)?.ifBlank { null },
            lastModified = lines.getOrNull(2)?.ifBlank { null },
        )
    }

    private fun saveValidators(key: String, url: String, etag: String?, lastModified: String?) {
        runCatching { validatorsFile(key).writeText("$url\n${etag.orEmpty()}\n${lastModified.orEmpty()}") }
            .onFailure { Log.w(TAG, "Failed to store app catalog validators ($key)", it) }
    }

    private data class Validators(val url: String?, val etag: String?, val lastModified: String?)

    private sealed interface FetchResult {
        /** Server returned new content (HTTP 200); [url] is the file it came from (localized or English). */
        data class Updated(val url: String, val body: String, val etag: String?, val lastModified: String?) : FetchResult

        /** Server reported the catalog is unchanged (HTTP 304); no body was transferred. */
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
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 15_000
    }
}
