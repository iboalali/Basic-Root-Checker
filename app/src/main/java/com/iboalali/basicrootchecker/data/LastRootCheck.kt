package com.iboalali.basicrootchecker.data

/**
 * The persisted record of the most recent root check. Recorded centrally by [RootChecker] on
 * every check, so it reflects checks triggered from the UI (the FAB) and from AppFunctions
 * (on-device agents) alike. Read back via [UserPreferences.lastRootCheck].
 */
data class LastRootCheck(
    /** `System.currentTimeMillis()` when the check ran. */
    val checkedAtEpochMs: Long,
    val status: RootCheckStatus,
    val provider: RootProvider?,
    /** Null when only a non-package signal fired. */
    val manager: RootManager?,
    val version: String?,
)

/**
 * Flattened outcome of a root check. Distinguishes [NOT_ROOTED] (confirmed no root) from
 * [UNKNOWN] (could not determine). Both mean "not currently rooted" but carry different
 * confidence.
 */
enum class RootCheckStatus { ROOTED, ROOTED_NOT_GRANTED, NOT_ROOTED, UNKNOWN }
