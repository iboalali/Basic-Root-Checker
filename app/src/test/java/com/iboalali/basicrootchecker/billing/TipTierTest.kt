package com.iboalali.basicrootchecker.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TipTierTest {

    @Test
    fun `launchProductId returns the record product when not yet owned`() {
        TipTier.entries.forEach { tier ->
            assertEquals(tier.recordProductId, tier.launchProductId(isRecordOwned = false))
        }
    }

    @Test
    fun `launchProductId returns the repeat product once the record is owned`() {
        TipTier.entries.forEach { tier ->
            assertEquals(tier.repeatProductId, tier.launchProductId(isRecordOwned = true))
        }
    }

    @Test
    fun `ALL_TIP_PRODUCT_IDS lists both variants of every tier with no duplicates`() {
        val expected = listOf(
            "tip_small", "tip_small_repeat",
            "tip_medium", "tip_medium_repeat",
            "tip_large", "tip_large_repeat",
        )
        assertEquals(expected, ALL_TIP_PRODUCT_IDS)
        assertEquals(ALL_TIP_PRODUCT_IDS.size, ALL_TIP_PRODUCT_IDS.toSet().size)
    }

    @Test
    fun `record and repeat product ids never collide across tiers`() {
        val recordIds = TipTier.entries.map { it.recordProductId }
        val repeatIds = TipTier.entries.map { it.repeatProductId }
        assertTrue(recordIds.intersect(repeatIds.toSet()).isEmpty())
    }

    @Test
    fun `tipTierForProductId maps both variants back to their tier`() {
        TipTier.entries.forEach { tier ->
            assertEquals(tier, tipTierForProductId(tier.recordProductId))
            assertEquals(tier, tipTierForProductId(tier.repeatProductId))
        }
    }

    @Test
    fun `tipTierForProductId returns null for unknown ids`() {
        assertNull(tipTierForProductId("not_a_tip"))
        assertNull(tipTierForProductId(""))
    }

    @Test
    fun `isRecordProductId is true only for the durable record variant`() {
        TipTier.entries.forEach { tier ->
            assertTrue(isRecordProductId(tier.recordProductId))
            assertFalse(isRecordProductId(tier.repeatProductId))
        }
        assertFalse(isRecordProductId("not_a_tip"))
    }
}
