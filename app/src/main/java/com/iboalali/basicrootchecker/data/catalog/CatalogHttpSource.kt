package com.iboalali.basicrootchecker.data.catalog

import java.io.Closeable
import java.io.File
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Fetches catalog JSON over a conditional GET, letting OkHttp's [Cache] own the HTTP revalidation:
 * it stores each response's `ETag`/`Last-Modified`, replays them as
 * `If-None-Match`/`If-Modified-Since`, and substitutes the cached body when the server answers `304`.
 * That replaces the hand-rolled validator sidecar files this used to keep, and gets correct `Vary`
 * handling and transparent gzip for free.
 *
 * Deliberately takes a plain [File] rather than a `Context`, so the caching contract it depends on is
 * unit-testable against a `MockWebServer` (see `CatalogHttpSourceTest`) — the previous
 * `HttpURLConnection` code was self-evident but untested; this one leans on library semantics that
 * are worth pinning down.
 *
 * Its client is **not** shared with Coil's. Coil brings its own (tuned for images), and pointing it
 * at this cache would push image traffic through a store sized for a few small JSON files and evict
 * the catalog.
 */
internal class CatalogHttpSource(cacheDir: File) : Closeable {

    private val client =
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .cache(Cache(cacheDir, CACHE_MAX_BYTES))
            .build()

    /**
     * Performs one conditional GET for [url].
     *
     * @return the payload, or `null` for a response to fall back on — any non-2xx, e.g. the `404` the
     *   feed contract specifies for a locale it doesn't publish.
     */
    fun fetch(url: String): CatalogPayload? {
        val request =
            Request.Builder()
                .url(url)
                // max-age=0 forces revalidation on every call while still sending the stored
                // validators, so a refresh always asks the server. Without it OkHttp would honour the
                // feed's own `Cache-Control: max-age=600` and answer from disk with no request at all
                // for ten minutes — correct per HTTP, but it would silently make refresh() a no-op
                // right after launch. Note this is NOT CacheControl.FORCE_NETWORK, which would skip
                // the conditional headers and re-download the whole body every time.
                .cacheControl(CacheControl.Builder().maxAge(0, TimeUnit.SECONDS).build())
                .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            // networkResponse is null when OkHttp answered from cache without a request at all; a 304
            // means it revalidated and the body it hands back is the cached one. Either way nothing
            // was transferred. The body is readable in both cases, so the caller can still rebuild a
            // lost snapshot from it.
            val network = response.networkResponse
            val transferred =
                network != null && network.code != HttpURLConnection.HTTP_NOT_MODIFIED
            return CatalogPayload(json = response.body.string(), transferred = transferred)
        }
    }

    override fun close() {
        client.cache?.close()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    private companion object {
        const val CONNECT_TIMEOUT_SECONDS = 10L
        const val READ_TIMEOUT_SECONDS = 15L
        // The feed is a handful of ~25 KB JSON files, one per locale; 1 MiB holds every locale with
        // room to spare.
        const val CACHE_MAX_BYTES = 1L * 1024 * 1024
    }
}

/**
 * One conditional GET's result.
 *
 * @param json the catalog payload. Always present — whether it arrived over the wire or came out of
 *   OkHttp's cache after a `304`.
 * @param transferred `true` only when the server actually sent a new body (a `200` from the network).
 *   `false` means nothing was downloaded: a `304` revalidation, or a cache hit that never hit the
 *   network.
 */
internal data class CatalogPayload(val json: String, val transferred: Boolean)
