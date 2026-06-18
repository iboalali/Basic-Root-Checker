package com.iboalali.basicrootchecker.appfunctions

import androidx.appfunctions.AppFunctionSerializable
import java.time.Instant

/**
 * The result of a device root check, in a flat shape an agent can read and reason about directly.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class RootStatus(
    /**
     * The outcome of the check, one of: "ROOTED" (root present and granted to this app),
     * "ROOTED_NOT_GRANTED" (root present but this app has not been allowed), "NOT_ROOTED"
     * (confirmed no root), or "UNKNOWN" (could not be determined).
     */
    val status: String,
    /** True when a root solution is present on the device, whether or not access was granted to this app. */
    val rooted: Boolean,
    /** True when this app has been granted root access (the superuser prompt was allowed). */
    val accessGranted: Boolean,
    /**
     * The detected root-solution family — "MAGISK", "KERNELSU", "APATCH", "OTHER", or "UNKNOWN" —
     * or null when no root was detected.
     */
    val provider: String?,
    /**
     * The specific root manager app when identifiable (e.g. "KITSUNE_MASK", "KERNELSU_NEXT"), or
     * null when only a generic signal (mount, path, or su binary) fired.
     */
    val manager: String?,
    /** The root manager version when known (e.g. the Magisk version), or null. */
    val version: String?,
    /** The point in time when this check was performed. */
    val checkedAt: Instant,
)
