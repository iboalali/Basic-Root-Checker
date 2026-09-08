package com.iboalali.basicrootchecker.appfunctions

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
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
 * **None of these functions declares an `AppFunctionContext` parameter, and none may.** On this
 * `@AppFunctionServiceEntryPoint` path the generated dispatch reads every argument out of the
 * parameter map that `AppFunctionExecutionDispatcher` builds from the *inventory metadata*, and the
 * context is not an agent-supplied argument, so KSP omits it from that metadata while still
 * emitting `parameters["appFunctionContext"] as AppFunctionContext` — which throws `null cannot be
 * cast to non-null type AppFunctionContext` on **every** call. Declaring it compiles, generates
 * valid-looking XML, and passes both unit tests and Play's upload validator, so only an on-device
 * `adb shell cmd app_function execute-app-function` call catches it. That is exactly how it reached
 * v2.5 unnoticed after v2.4 had worked.
 *
 * Nothing here needs the caller's identity, so the parameter is simply left off and each adapter
 * passes the service's own `applicationContext` down instead.
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
     * @return The current root status of the device.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun checkRootStatus(): RootStatus = functions.checkRootStatus(applicationContext)

    /**
     * Request root access for this app, then return the resulting root status. If root is installed
     * but not yet allowed for this app, the device's superuser dialog (Magisk, KernelSU, or APatch)
     * appears and the user must approve it on the device, so this is not fully hands-free. To read
     * the current state without prompting, use checkRootStatus.
     *
     * @return The root status after the access request.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun requestRootAccess(): RootStatus = functions.requestRootAccess(applicationContext)

    /**
     * Return the most recent root check — its result and the time it ran — without re-probing the
     * device. Read the checkedAt field to report when the last check happened.
     *
     * @return The last root status, or null if no check has ever run on this device.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getLastRootCheck(): RootStatus? = functions.getLastRootCheck(applicationContext)
}
