package com.iboalali.basicrootchecker.appfunctions

import androidx.appfunctions.AppFunctionContext
import com.iboalali.basicrootchecker.data.LastRootCheck
import com.iboalali.basicrootchecker.data.RootCheckStatus
import com.iboalali.basicrootchecker.data.RootChecker
import com.iboalali.basicrootchecker.data.RootResult
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
 * it can be exercised directly. Each function obtains the Android [android.content.Context] it
 * needs from [AppFunctionContext.context].
 */
class RootAppFunctions {

    /** Runs a fresh passive root check and maps it to the agent-facing [RootStatus]. */
    suspend fun checkRootStatus(appFunctionContext: AppFunctionContext): RootStatus {
        val result = RootChecker.check(appFunctionContext.context, applyUiDelay = false)
        return result.toRootStatus(Instant.now())
    }

    /** Forces the superuser prompt, then maps the resulting state to a [RootStatus]. */
    suspend fun requestRootAccess(appFunctionContext: AppFunctionContext): RootStatus {
        val result = RootChecker.requestRoot(appFunctionContext.context, applyUiDelay = false)
        return result.toRootStatus(Instant.now())
    }

    /** Reads the last recorded check from [UserPreferences] without re-probing the device. */
    suspend fun getLastRootCheck(appFunctionContext: AppFunctionContext): RootStatus? {
        return UserPreferences(appFunctionContext.context).lastRootCheck.first()?.toRootStatus()
    }
}

private fun RootResult.toRootStatus(checkedAt: Instant): RootStatus =
    when (this) {
        is RootResult.Rooted ->
            RootStatus(
                status = "ROOTED",
                rooted = true,
                accessGranted = true,
                provider = provider.name,
                manager = manager?.name,
                version = version,
                checkedAt = checkedAt,
            )

        is RootResult.RootedNotGranted ->
            RootStatus(
                status = "ROOTED_NOT_GRANTED",
                rooted = true,
                accessGranted = false,
                provider = provider.name,
                manager = manager?.name,
                version = null,
                checkedAt = checkedAt,
            )

        RootResult.NotRooted ->
            RootStatus(
                status = "NOT_ROOTED",
                rooted = false,
                accessGranted = false,
                provider = null,
                manager = null,
                version = null,
                checkedAt = checkedAt,
            )

        RootResult.Unknown ->
            RootStatus(
                status = "UNKNOWN",
                rooted = false,
                accessGranted = false,
                provider = null,
                manager = null,
                version = null,
                checkedAt = checkedAt,
            )
    }

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
