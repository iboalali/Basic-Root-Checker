package com.iboalali.basicrootchecker.appfunctions

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.service.AppFunction
import com.iboalali.basicrootchecker.data.LastRootCheck
import com.iboalali.basicrootchecker.data.RootCheckStatus
import com.iboalali.basicrootchecker.data.RootChecker
import com.iboalali.basicrootchecker.data.RootResult
import com.iboalali.basicrootchecker.data.UserPreferences
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * Basic Root Checker's [AppFunction]s — the app's root-check workflows exposed to the Android
 * system and to on-device agents, so a device's root state can be queried hands-free without
 * opening the app.
 *
 * Instantiated by the AppFunctions framework via its no-arg constructor; each function obtains
 * the Android [android.content.Context] it needs from [AppFunctionContext.context].
 */
class RootAppFunctions {

    /**
     * Run a fresh root check on this device and return its current root status. Re-probes the
     * device on every call. To read the previous check and the time it ran without re-probing,
     * use getLastRootCheck.
     *
     * @param appFunctionContext The execution context.
     * @return The current root status of the device.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun checkRootStatus(appFunctionContext: AppFunctionContext): RootStatus {
        val result = RootChecker.check(appFunctionContext.context, applyUiDelay = false)
        return result.toRootStatus(Instant.now())
    }

    /**
     * Request root access for this app, then return the resulting root status. If root is
     * installed but not yet allowed for this app, the device's superuser dialog (Magisk, KernelSU,
     * or APatch) appears and the user must approve it on the device, so this is not fully
     * hands-free. To read the current state without prompting, use checkRootStatus.
     *
     * @param appFunctionContext The execution context.
     * @return The root status after the access request.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun requestRootAccess(appFunctionContext: AppFunctionContext): RootStatus {
        val result = RootChecker.requestRoot(appFunctionContext.context, applyUiDelay = false)
        return result.toRootStatus(Instant.now())
    }

    /**
     * Return the most recent root check — its result and the time it ran — without re-probing the
     * device. Read the checkedAt field to report when the last check happened.
     *
     * @param appFunctionContext The execution context.
     * @return The last root status, or null if no check has ever run on this device.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getLastRootCheck(appFunctionContext: AppFunctionContext): RootStatus? {
        return UserPreferences(appFunctionContext.context).lastRootCheck.first()?.toRootStatus()
    }
}

private fun RootResult.toRootStatus(checkedAt: Instant): RootStatus = when (this) {
    is RootResult.Rooted -> RootStatus(
        status = "ROOTED",
        rooted = true,
        accessGranted = true,
        provider = provider.name,
        manager = manager?.name,
        version = version,
        checkedAt = checkedAt,
    )

    is RootResult.RootedNotGranted -> RootStatus(
        status = "ROOTED_NOT_GRANTED",
        rooted = true,
        accessGranted = false,
        provider = provider.name,
        manager = manager?.name,
        version = null,
        checkedAt = checkedAt,
    )

    RootResult.NotRooted -> RootStatus(
        status = "NOT_ROOTED",
        rooted = false,
        accessGranted = false,
        provider = null,
        manager = null,
        version = null,
        checkedAt = checkedAt,
    )

    RootResult.Unknown -> RootStatus(
        status = "UNKNOWN",
        rooted = false,
        accessGranted = false,
        provider = null,
        manager = null,
        version = null,
        checkedAt = checkedAt,
    )
}

private fun LastRootCheck.toRootStatus(): RootStatus = RootStatus(
    status = status.name,
    rooted = status == RootCheckStatus.ROOTED || status == RootCheckStatus.ROOTED_NOT_GRANTED,
    accessGranted = status == RootCheckStatus.ROOTED,
    provider = provider?.name,
    manager = manager?.name,
    version = version,
    checkedAt = Instant.ofEpochMilli(checkedAtEpochMs),
)
