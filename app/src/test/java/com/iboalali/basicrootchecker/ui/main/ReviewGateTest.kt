package com.iboalali.basicrootchecker.ui.main

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewGateTest {

    @Test
    fun `not eligible below the rooted-check threshold`() {
        assertFalse(ReviewGate.shouldRequest(rootedCount = 0, lastPromptedVersion = 0, currentVersion = 5))
        assertFalse(
            ReviewGate.shouldRequest(
                rootedCount = ReviewGate.MIN_ROOTED_CHECKS - 1,
                lastPromptedVersion = 0,
                currentVersion = 5,
            )
        )
    }

    @Test
    fun `eligible at the threshold when never prompted`() {
        assertTrue(
            ReviewGate.shouldRequest(
                rootedCount = ReviewGate.MIN_ROOTED_CHECKS,
                lastPromptedVersion = 0,
                currentVersion = 5,
            )
        )
    }

    @Test
    fun `not eligible again once prompted on the current version`() {
        assertFalse(
            ReviewGate.shouldRequest(
                rootedCount = ReviewGate.MIN_ROOTED_CHECKS + 10,
                lastPromptedVersion = 5,
                currentVersion = 5,
            )
        )
    }

    @Test
    fun `eligible again on a newer version after a prior prompt`() {
        assertTrue(
            ReviewGate.shouldRequest(
                rootedCount = ReviewGate.MIN_ROOTED_CHECKS,
                lastPromptedVersion = 5,
                currentVersion = 6,
            )
        )
    }
}
