package com.iboalali.basicrootchecker.data

/**
 * The persisted record of the most recent root check. Recorded centrally by [RootChecker] on
 * every check, so it reflects checks triggered from the UI (the FAB) and from AppFunctions
 * (on-device agents) alike. Read back via [UserPreferences.lastRootCheck].
 */
data class LastRootCheck(
    /** When the check ran, as epoch milliseconds (`System.currentTimeMillis()`). */
    val checkedAtEpochMs: Long,
    /** The outcome of the check. */
    val status: RootCheckStatus,
    /** Detected root-solution family, or null when none was identified. */
    val provider: RootProvider?,
    /** Specific root manager app, or null when only a non-package signal fired. */
    val manager: RootManager?,
    /** Root manager version when known (e.g. Magisk version), or null. */
    val version: String?,
)

/**
 * Flattened outcome of a root check. Distinguishes [NOT_ROOTED] (confirmed no root) from
 * [UNKNOWN] (could not determine) — both of which mean "not currently rooted" but carry
 * different confidence.
 */
enum class RootCheckStatus { ROOTED, ROOTED_NOT_GRANTED, NOT_ROOTED, UNKNOWN }
