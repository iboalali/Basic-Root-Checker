package com.iboalali.basicrootchecker.ui.main

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportGateTest {

    /** The all-clear case; each test below flips exactly one input to prove it gates. */
    private fun shouldShow(
        billingAvailable: Boolean = true,
        productsLoaded: Boolean = true,
        alreadySupporter: Boolean = false,
        rootedCount: Int = SupportGate.MIN_ROOTED_CHECKS,
        dismissCount: Int = 0,
        snoozedUntilEpochMs: Long = 0L,
        nowEpochMs: Long = 1_000_000L,
        reviewRequestedThisSession: Boolean = false,
        updatePending: Boolean = false,
    ): Boolean = SupportGate.shouldShow(
        billingAvailable = billingAvailable,
        productsLoaded = productsLoaded,
        alreadySupporter = alreadySupporter,
        rootedCount = rootedCount,
        dismissCount = dismissCount,
        snoozedUntilEpochMs = snoozedUntilEpochMs,
        nowEpochMs = nowEpochMs,
        reviewRequestedThisSession = reviewRequestedThisSession,
        updatePending = updatePending,
    )

    @Test
    fun `eligible at the threshold with nothing blocking`() {
        assertTrue(shouldShow())
    }

    @Test
    fun `not eligible below the rooted-check threshold`() {
        assertFalse(shouldShow(rootedCount = 0))
        assertFalse(shouldShow(rootedCount = SupportGate.MIN_ROOTED_CHECKS - 1))
    }

    @Test
    fun `threshold sits above the review gate so review is asked first`() {
        assertTrue(SupportGate.MIN_ROOTED_CHECKS > ReviewGate.MIN_ROOTED_CHECKS)
        assertFalse(shouldShow(rootedCount = ReviewGate.MIN_ROOTED_CHECKS))
    }

    @Test
    fun `never shown without billing or loaded prices`() {
        assertFalse(shouldShow(billingAvailable = false))
        assertFalse(shouldShow(productsLoaded = false))
    }

    @Test
    fun `never shown to someone who already tipped`() {
        assertFalse(shouldShow(alreadySupporter = true))
    }

    @Test
    fun `not shown in the same session as the review prompt`() {
        assertFalse(shouldShow(reviewRequestedThisSession = true))
    }

    @Test
    fun `the update card owns the slot while an update is pending`() {
        assertFalse(shouldShow(updatePending = true))
    }

    @Test
    fun `snoozed until the window passes`() {
        assertFalse(shouldShow(nowEpochMs = 500L, snoozedUntilEpochMs = 1_000L))
        assertTrue(shouldShow(nowEpochMs = 1_000L, snoozedUntilEpochMs = 1_000L))
        assertTrue(shouldShow(nowEpochMs = 1_001L, snoozedUntilEpochMs = 1_000L))
    }

    @Test
    fun `gone for good after the dismissal cap`() {
        assertTrue(shouldShow(dismissCount = SupportGate.MAX_DISMISSALS - 1))
        assertFalse(shouldShow(dismissCount = SupportGate.MAX_DISMISSALS))
        assertFalse(shouldShow(dismissCount = SupportGate.MAX_DISMISSALS + 1))
    }

    @Test
    fun `snoozeUntil pushes eligibility a full window out`() {
        val now = 1_700_000_000_000L
        val until = SupportGate.snoozeUntil(now)
        assertTrue(until == now + SupportGate.SNOOZE_MILLIS)
        assertFalse(shouldShow(nowEpochMs = now, snoozedUntilEpochMs = until))
        assertTrue(shouldShow(nowEpochMs = until, snoozedUntilEpochMs = until))
    }
}
