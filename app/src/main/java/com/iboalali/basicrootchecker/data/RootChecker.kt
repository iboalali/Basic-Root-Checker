package com.iboalali.basicrootchecker.data

import android.content.Context
import android.content.pm.PackageManager
import com.iboalali.basicrootchecker.analytics.Analytics
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

sealed interface RootResult {
    data object NotRooted : RootResult
    data object Unknown : RootResult
    data class Rooted(
        val provider: RootProvider,
        val manager: RootManager?,
        val version: String?,
    ) : RootResult

    data class RootedNotGranted(
        val provider: RootProvider,
        val manager: RootManager?,
    ) : RootResult
}

/** Coarse root-solution family, used for detection logic (Magisk-only probes) and analytics. */
enum class RootProvider { MAGISK, KERNELSU, APATCH, OTHER, UNKNOWN }

/**
 * A specific, named root manager identified by its installed package, mapped to its [provider]
 * family. Carried through to the UI so users see the exact manager they installed (e.g. "Kitsune
 * Mask") rather than just the family ("Magisk"). Null when only a non-package signal (mount, path,
 * su binary, or `magisk -v`) fired, in which case the UI falls back to the family name.
 */
enum class RootManager(val provider: RootProvider) {
    MAGISK(RootProvider.MAGISK),
    KITSUNE_MASK(RootProvider.MAGISK), // Kitsune Mask / Magisk Delta
    KERNELSU(RootProvider.KERNELSU),
    KERNELSU_NEXT(RootProvider.KERNELSU),
    SUKISU_ULTRA(RootProvider.KERNELSU),
    RESUKISU(RootProvider.KERNELSU),
    APATCH(RootProvider.APATCH),
    SUPERSU(RootProvider.OTHER),
    SUPERUSER(RootProvider.OTHER),
    KINGROOT(RootProvider.OTHER),
    PHH(RootProvider.OTHER),
}

internal data class RootSignals(
    val granted: Boolean?,
    val packageManager: RootManager?,
    val magiskMountHit: Boolean,
    val magiskFilesHit: Boolean,
    val magiskPathHit: Boolean,
    val magiskVersion: String?,
    val suBinaryHit: Boolean,
)

internal fun classify(signals: RootSignals): RootResult = with(signals) {
    val managerFamily = packageManager?.provider
    // Only surface the specific manager when its family matches the family we resolve below; a magisk
    // mount/path can force MAGISK even on a device whose installed manager app is, say, KernelSU, and
    // we must not then claim that KernelSU app is the Magisk we detected.
    fun manager(provider: RootProvider) = packageManager?.takeIf { it.provider == provider }
    when (granted) {
        true -> {
            val magiskCertain = magiskVersion != null ||
                    magiskMountHit ||
                    magiskFilesHit ||
                    magiskPathHit ||
                    managerFamily == RootProvider.MAGISK
            val provider = when {
                magiskCertain -> RootProvider.MAGISK
                managerFamily != null -> managerFamily
                else -> RootProvider.OTHER
            }
            RootResult.Rooted(provider, manager(provider), magiskVersion)
        }

        false, null -> {
            val provider = when {
                magiskMountHit || magiskPathHit || managerFamily == RootProvider.MAGISK -> RootProvider.MAGISK
                managerFamily == RootProvider.KERNELSU -> RootProvider.KERNELSU
                managerFamily == RootProvider.APATCH -> RootProvider.APATCH
                suBinaryHit || managerFamily == RootProvider.OTHER -> RootProvider.OTHER
                else -> null
            }
            when {
                provider != null -> RootResult.RootedNotGranted(provider, manager(provider))
                granted == false -> RootResult.NotRooted
                else -> RootResult.Unknown
            }
        }
    }
}

internal fun parseMagiskVersionCode(code: Long): String {
    val major = code / 1000
    val minor = (code % 1000) / 100
    return "$major.$minor"
}

object RootChecker {

    // Known manager package ids → the specific manager they identify. Iteration order is the
    // tie-break when more than one is installed (rare), so it mirrors the family priority in
    // classify: Magisk family, then KernelSU family, then APatch, then legacy managers.
    // Every id here must also be declared in AndroidManifest's <queries> block, or it is invisible
    // to PackageManager on API 30+.
    private val PACKAGE_MANAGERS: Map<String, RootManager> = linkedMapOf(
        "com.topjohnwu.magisk" to RootManager.MAGISK,
        "com.topjohnwu.magisk.debug" to RootManager.MAGISK,
        "com.topjohnwu.magisk.canary" to RootManager.MAGISK,
        "com.topjohnwu.magisk.alpha" to RootManager.MAGISK,
        "io.github.huskydg.magisk" to RootManager.KITSUNE_MASK,
        "me.weishu.kernelsu" to RootManager.KERNELSU,
        "com.rifsxd.ksunext" to RootManager.KERNELSU_NEXT,
        "com.sukisu.ultra" to RootManager.SUKISU_ULTRA,
        "com.resukisu.resukisu" to RootManager.RESUKISU,
        "me.bmax.apatch" to RootManager.APATCH,
        // Legacy managers. These usually ship a real su binary at standard paths (caught by
        // probeSuBinary), so a package hit mainly upgrades the result from a bare OTHER to a named
        // manager — and catches the rare case where the binary path is not stat-able.
        "eu.chainfire.supersu" to RootManager.SUPERSU,
        "com.koushikdutta.superuser" to RootManager.SUPERUSER,
        "com.noshufou.android.su" to RootManager.SUPERUSER,
        "com.noshufou.android.su.elite" to RootManager.SUPERUSER,
        "com.kingroot.kinguser" to RootManager.KINGROOT,
        "com.kingouser.com" to RootManager.KINGROOT,
        "me.phh.superuser" to RootManager.PHH,
    )

    private val SU_PATHS = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/su/bin/su",
        "/su/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/magisk/.core/bin/su",
    )

    // Directories left by Magisk and its forks. Stat-ability is device/SELinux dependent, so a hit
    // is a best-effort fingerprint, not a guarantee. Probed unprivileged (no root required).
    private val MAGISK_PATHS = listOf(
        "/data/adb/magisk",
        "/data/adb/modules",
        "/sbin/.magisk",
        "/debug_ramdisk/.magisk",
    )

    /**
     * Evaluate root passively. [applyUiDelay] adds a ~1s settle delay so the UI's "checking"
     * animation has time to play; callers that want an immediate result (e.g. AppFunctions
     * invoked by an agent) pass `false`.
     */
    suspend fun check(context: Context, applyUiDelay: Boolean = true): RootResult = withContext(Dispatchers.IO) {
        val result = classify(collectSignals(context))
        recordCheck(context, result)
        if (applyUiDelay) delay(1000.milliseconds)
        result
    }

    suspend fun requestRoot(context: Context, applyUiDelay: Boolean = true): RootResult = withContext(Dispatchers.IO) {
        // Forces libsu to construct its main shell, which prompts the Magisk/KernelSU/APatch
        // allow dialog if the user has not yet made a decision.
        Shell.cmd("id").exec()
        val result = classify(collectSignals(context))
        recordCheck(context, result)
        if (applyUiDelay) delay(1000.milliseconds)
        result
    }

    /** Persist [result] as the last root check so any caller (UI or AppFunction) shares it. */
    private suspend fun recordCheck(context: Context, result: RootResult) {
        UserPreferences(context).recordRootCheck(result.toRecord(System.currentTimeMillis()))
    }

    private fun RootResult.toRecord(checkedAtEpochMs: Long): LastRootCheck = when (this) {
        is RootResult.Rooted ->
            LastRootCheck(checkedAtEpochMs, RootCheckStatus.ROOTED, provider, manager, version)
        is RootResult.RootedNotGranted ->
            LastRootCheck(checkedAtEpochMs, RootCheckStatus.ROOTED_NOT_GRANTED, provider, manager, null)
        RootResult.NotRooted ->
            LastRootCheck(checkedAtEpochMs, RootCheckStatus.NOT_ROOTED, null, null, null)
        RootResult.Unknown ->
            LastRootCheck(checkedAtEpochMs, RootCheckStatus.UNKNOWN, null, null, null)
    }

    private fun collectSignals(context: Context): RootSignals {
        val granted = Shell.isAppGrantedRoot()
        return RootSignals(
            granted = granted,
            packageManager = detectInstalledManager(context),
            magiskMountHit = probeMagiskMounts(),
            magiskFilesHit = if (granted == true) probeMagiskFiles() else false,
            magiskPathHit = probeMagiskPaths(),
            magiskVersion = if (granted == true) queryMagiskVersion() else null,
            suBinaryHit = probeSuBinary(),
        )
    }

    /** First installed manager in [PACKAGE_MANAGERS] order (family priority), or null if none. */
    private fun detectInstalledManager(context: Context): RootManager? {
        val pm = context.packageManager
        return PACKAGE_MANAGERS.entries.firstOrNull { (id, _) -> isPackageInstalled(pm, id) }?.value
    }

    private fun isPackageInstalled(pm: PackageManager, name: String): Boolean = try {
        pm.getPackageInfo(name, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun probeMagiskMounts(): Boolean = try {
        File("/proc/self/mounts").readText().contains("magisk", ignoreCase = true)
    } catch (e: Exception) {
        Analytics.trackError(e, id = "probeMagiskMounts")
        false
    }

    private fun probeSuBinary(): Boolean = SU_PATHS.any { path ->
        try {
            File(path).exists()
        } catch (e: SecurityException) {
            Analytics.trackError(e, id = "probeSuBinary")
            false
        }
    }

    private fun probeMagiskPaths(): Boolean = MAGISK_PATHS.any { path ->
        try {
            File(path).exists()
        } catch (e: SecurityException) {
            Analytics.trackError(e, id = "probeMagiskPaths")
            false
        }
    }

    private fun probeMagiskFiles(): Boolean {
        val result = Shell.cmd(
            "test -d /data/adb/magisk || test -d /debug_ramdisk/.magisk || test -d /sbin/.magisk || test -d /data/adb/modules"
        ).exec()
        return result.isSuccess
    }

    private fun queryMagiskVersion(): String? {
        val nameResult = Shell.cmd("magisk -v").exec()
        if (nameResult.isSuccess) {
            val name = nameResult.out.firstOrNull()?.trim().orEmpty()
            if (name.isNotEmpty()) return name
        }
        val codeResult = Shell.cmd("magisk -V").exec()
        if (codeResult.isSuccess) {
            val code = codeResult.out.firstOrNull()?.trim()?.toLongOrNull()
            if (code != null) return parseMagiskVersionCode(code)
        }
        return null
    }
}
