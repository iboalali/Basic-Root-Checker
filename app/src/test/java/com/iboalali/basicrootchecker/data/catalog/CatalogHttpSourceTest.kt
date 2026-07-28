package com.iboalali.basicrootchecker.data.catalog

import java.nio.file.Files
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pins the caching behaviour [CatalogHttpSource] delegates to OkHttp. The hand-rolled
 * `HttpURLConnection` version it replaced set `If-None-Match` in plain sight; this one relies on
 * library semantics, and two of them are load-bearing and easy to get subtly wrong:
 * - a repeat call must send a **conditional** GET (not re-download, and not skip the server), and
 * - a `304` must surface as "nothing transferred" while still yielding the cached body.
 */
class CatalogHttpSourceTest {

    private lateinit var server: MockWebServer
    private lateinit var source: CatalogHttpSource

    private val catalogJson = """{"locale":"en","apps":[{"name":"Billboard"}]}"""

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        source = CatalogHttpSource(Files.createTempDirectory("catalog-http").toFile())
    }

    @After
    fun tearDown() {
        source.close()
        server.close()
    }

    private fun url() = server.url("/apps.json").toString()

    /** A response shaped like the real feed: validators plus a freshness window. */
    private fun catalogResponse() =
        MockResponse.Builder()
            .code(200)
            .addHeader("ETag", "\"v1\"")
            .addHeader("Cache-Control", "max-age=600")
            .body(catalogJson)
            .build()

    @Test
    fun `first fetch reports a transferred body`() {
        server.enqueue(catalogResponse())

        val payload = source.fetch(url())

        assertEquals(catalogJson, payload?.json)
        assertEquals(true, payload?.transferred)
    }

    @Test
    fun `repeat fetch revalidates conditionally instead of re-downloading`() {
        server.enqueue(catalogResponse())
        server.enqueue(MockResponse.Builder().code(304).build())

        source.fetch(url())
        val payload = source.fetch(url())

        // The point of the exercise: the server was asked, and asked *conditionally*. Without the
        // max-age=0 request override OkHttp would have honoured max-age=600 and never sent this.
        assertEquals(2, server.requestCount)
        server.takeRequest() // the initial 200
        val revalidation = server.takeRequest()
        assertEquals("\"v1\"", revalidation.headers["If-None-Match"])

        // A 304 transfers no body, but OkHttp still hands back the cached one.
        assertEquals(false, payload?.transferred)
        assertEquals(catalogJson, payload?.json)
    }

    @Test
    fun `304 keeps serving the cached body after the server stops sending one`() {
        server.enqueue(catalogResponse())
        repeat(3) { server.enqueue(MockResponse.Builder().code(304).build()) }

        source.fetch(url())
        repeat(3) {
            val payload = source.fetch(url())
            assertEquals(catalogJson, payload?.json)
            assertEquals(false, payload?.transferred)
        }
    }

    @Test
    fun `a changed catalog is transferred again`() {
        val updated = """{"locale":"en","apps":[{"name":"Billboard"},{"name":"Icon Recomposer"}]}"""
        server.enqueue(catalogResponse())
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("ETag", "\"v2\"")
                .addHeader("Cache-Control", "max-age=600")
                .body(updated)
                .build()
        )

        source.fetch(url())
        val payload = source.fetch(url())

        assertEquals(updated, payload?.json)
        assertEquals(true, payload?.transferred)
    }

    @Test
    fun `a 404 returns null so the caller can fall back to English`() {
        server.enqueue(MockResponse.Builder().code(404).build())

        assertNull(source.fetch(url()))
    }

    @Test
    fun `a 500 returns null rather than throwing`() {
        server.enqueue(MockResponse.Builder().code(500).build())

        assertNull(source.fetch(url()))
    }

    @Test
    fun `an uncacheable response still fetches every time`() {
        // no-store means OkHttp keeps nothing, so every call is a full download. The catalog stays
        // correct, it just loses the bandwidth saving -- worth knowing rather than assuming.
        repeat(2) {
            server.enqueue(
                MockResponse.Builder().code(200).addHeader("Cache-Control", "no-store")
                    .body(catalogJson).build()
            )
        }

        source.fetch(url())
        val payload = source.fetch(url())

        assertEquals(catalogJson, payload?.json)
        assertTrue("a no-store response must not be reported as cached", payload!!.transferred)
    }
}
