package com.iboalali.basicrootchecker.appfunctions

import android.content.Context
import com.iboalali.basicrootchecker.data.LastRootCheck
import com.iboalali.basicrootchecker.data.RootCheckStatus
import com.iboalali.basicrootchecker.data.RootChecker
import com.iboalali.basicrootchecker.data.UserPreferences
import java.time.Instant
import kotlinx.coroutines.flow.first

/**
 * The implementation behind Basic Root Checker's AppFunctions — the app's root-check workflows
 * exposed to the Android system and to on-device agents, so a device's root state can be queried
 * hands-free without opening the app.
 *
 * The `@AppFunction` annotations and the agent-facing KDoc live on [BaseRootAppFunctionService],
 * which delegates here; this class stays plain (no service lifecycle, no framework annotations) so
 * it can be exercised directly.
 *
 * Each function takes the Android [Context] as a plain parameter — the service passes its own
 * `applicationContext`. Deliberately **not** an `AppFunctionContext`: declaring one of those on the
 * annotated entry point throws on every call, and taking a plain [Context] here also keeps this
 * class callable from a test. See [BaseRootAppFunctionService] for the mechanism.
 */
class RootAppFunctions {

    /** Runs a fresh passive root check and maps it to the agent-facing [RootStatus]. */
    suspend fun checkRootStatus(context: Context): RootStatus =
        RootChecker.checkRecorded(context, applyUiDelay = false).record.toRootStatus()

    /** Forces the superuser prompt, then maps the resulting state to a [RootStatus]. */
    suspend fun requestRootAccess(context: Context): RootStatus =
        RootChecker.requestRootRecorded(context, applyUiDelay = false).record.toRootStatus()

    /** Reads the last recorded check from [UserPreferences] without re-probing the device. */
    suspend fun getLastRootCheck(context: Context): RootStatus? =
        UserPreferences(context).lastRootCheck.first()?.toRootStatus()
}

/**
 * The one mapping from a persisted check to the agent-facing shape. All three functions above go
 * through it, so whichever one an agent calls, the same check reports the same `checkedAt` — the
 * instant [RootChecker] recorded, never a second reading of the clock.
 */
private fun LastRootCheck.toRootStatus(): RootStatus =
    RootStatus(
        status = status.name,
        rooted = status == RootCheckStatus.ROOTED || status == RootCheckStatus.ROOTED_NOT_GRANTED,
        accessGranted = status == RootCheckStatus.ROOTED,
        provider = provider?.name,
        manager = manager?.name,
        version = version,
        checkedAt = Instant.ofEpochMilli(checkedAtEpochMs),
    )
