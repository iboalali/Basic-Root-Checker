package com.iboalali.basicrootchecker.appfunctions

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

/**
 * AppFunctions entry point (androidx.appfunctions 1.0.0-alpha10). The
 * `@AppFunctionServiceEntryPoint` compiler generates the concrete `RootAppFunctionService`
 * (declared in the manifest) and the `assets/root_app_function_service.xml` metadata from the
 * `@AppFunction` methods below.
 *
 * These methods are thin adapters: the actual probing and mapping lives in [RootAppFunctions],
 * which stays a plain class so it remains testable without the Android service lifecycle. The
 * agent-facing contract — the KDoc that becomes each function's description in the generated XML —
 * lives here on the annotated methods.
 *
 * Each function gets the Android [android.content.Context] it needs from
 * [AppFunctionContext.context] rather than the service, so the adapters stay side-effect free.
 */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "RootAppFunctionService",
    appFunctionXmlFileName = "root_app_function_service",
)
abstract class BaseRootAppFunctionService : AppFunctionService() {

    private val functions = RootAppFunctions()

    /**
     * Run a fresh root check on this device and return its current root status. Re-probes the
     * device on every call. To read the previous check and the time it ran without re-probing, use
     * getLastRootCheck.
     *
     * @param appFunctionContext The execution context.
     * @return The current root status of the device.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun checkRootStatus(appFunctionContext: AppFunctionContext): RootStatus =
        functions.checkRootStatus(appFunctionContext)

    /**
     * Request root access for this app, then return the resulting root status. If root is installed
     * but not yet allowed for this app, the device's superuser dialog (Magisk, KernelSU, or APatch)
     * appears and the user must approve it on the device, so this is not fully hands-free. To read
     * the current state without prompting, use checkRootStatus.
     *
     * @param appFunctionContext The execution context.
     * @return The root status after the access request.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun requestRootAccess(appFunctionContext: AppFunctionContext): RootStatus =
        functions.requestRootAccess(appFunctionContext)

    /**
     * Return the most recent root check — its result and the time it ran — without re-probing the
     * device. Read the checkedAt field to report when the last check happened.
     *
     * @param appFunctionContext The execution context.
     * @return The last root status, or null if no check has ever run on this device.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getLastRootCheck(appFunctionContext: AppFunctionContext): RootStatus? =
        functions.getLastRootCheck(appFunctionContext)
}
