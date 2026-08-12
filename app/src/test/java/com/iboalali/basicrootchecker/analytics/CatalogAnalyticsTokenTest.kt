package com.iboalali.basicrootchecker.analytics

import com.iboalali.appcatalog.data.CatalogRefreshResult
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the `appCatalogRefresh` token values this app emits.
 *
 * The catalog moved to the shared `com.iboalali.appcatalog:data` module, which emits `notModified`
 * (Billboard's spelling) where this app has always reported `not_modified`. The mapping under test
 * is the only thing keeping that migration from silently rewriting a value every historical signal
 * and TelemetryDeck query here depends on — and nothing about a wrong mapping would fail a build or
 * look wrong on screen.
 */
class CatalogAnalyticsTokenTest {

    @Test
    fun `notModified is reported with this app's snake_case spelling`() {
        assertEquals(
            "not_modified",
            catalogResultToAnalyticsToken(CatalogRefreshResult.NOT_MODIFIED),
        )
    }

    @Test
    fun `updated and failure already agree with the library`() {
        assertEquals("updated", catalogResultToAnalyticsToken(CatalogRefreshResult.UPDATED))
        assertEquals("failure", catalogResultToAnalyticsToken(CatalogRefreshResult.FAILURE))
    }

    @Test
    fun `every library token maps to this app's own constant`() {
        assertEquals(
            CATALOG_REFRESH_UPDATED,
            catalogResultToAnalyticsToken(CatalogRefreshResult.UPDATED),
        )
        assertEquals(
            CATALOG_REFRESH_NOT_MODIFIED,
            catalogResultToAnalyticsToken(CatalogRefreshResult.NOT_MODIFIED),
        )
        assertEquals(
            CATALOG_REFRESH_FAILURE,
            catalogResultToAnalyticsToken(CatalogRefreshResult.FAILURE),
        )
    }

    @Test
    fun `an unknown token passes through rather than vanishing`() {
        // A token added to the library later should still reach telemetry, not be silently dropped.
        assertEquals("somethingNew", catalogResultToAnalyticsToken("somethingNew"))
    }
}
