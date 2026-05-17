package com.iboalali.basicrootchecker.data

import android.content.Context
import android.content.pm.PackageManager
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

sealed interface RootResult {
    data object NotRooted : RootResult
    data object Unknown : RootResult
    data class Rooted(val provider: RootProvider, val version: String?) : RootResult
    data class RootedNotGranted(val provider: RootProvider) : RootResult
}

enum class RootProvider { MAGISK, KERNELSU, APATCH, OTHER, UNKNOWN }

object RootChecker {

    private val MAGISK_PACKAGES = listOf(
        "com.topjohnwu.magisk",
        "com.topjohnwu.magisk.debug",
        "com.topjohnwu.magisk.canary",
        "com.topjohnwu.magisk.alpha",
    )

    private val KERNELSU_PACKAGES = listOf(
        "me.weishu.kernelsu",
        "com.rifsxd.ksunext",
    )

    private val APATCH_PACKAGES = listOf(
        "me.bmax.apatch",
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

    suspend fun check(context: Context): RootResult = withContext(Dispatchers.IO) {
        val result = evaluate(context)
        delay(1000.milliseconds)
        result
    }

    suspend fun requestRoot(context: Context): RootResult = withContext(Dispatchers.IO) {
        // Forces libsu to construct its main shell, which prompts the Magisk/KernelSU/APatch
        // allow dialog if the user has not yet made a decision.
        Shell.cmd("id").exec()
        val result = evaluate(context)
        delay(1000.milliseconds)
        result
    }

    private fun evaluate(context: Context): RootResult {
        val granted = Shell.isAppGrantedRoot()
        val magiskPackage = probeAnyPackage(context, MAGISK_PACKAGES)
        val magiskMount = probeMagiskMounts()
        val kernelsuPackage = probeAnyPackage(context, KERNELSU_PACKAGES)
        val apatchPackage = probeAnyPackage(context, APATCH_PACKAGES)
        val suBinary = probeSuBinary()

        return when (granted) {
            true -> {
                val version = queryMagiskVersion()
                val magiskCertain = version != null || magiskPackage || magiskMount || probeMagiskFiles()
                val provider = when {
                    magiskCertain -> RootProvider.MAGISK
                    kernelsuPackage -> RootProvider.KERNELSU
                    apatchPackage -> RootProvider.APATCH
                    else -> RootProvider.OTHER
                }
                RootResult.Rooted(provider, version)
            }

            false, null -> {
                val provider = when {
                    magiskPackage || magiskMount -> RootProvider.MAGISK
                    kernelsuPackage -> RootProvider.KERNELSU
                    apatchPackage -> RootProvider.APATCH
                    suBinary -> RootProvider.OTHER
                    else -> null
                }
                when {
                    provider != null -> RootResult.RootedNotGranted(provider)
                    granted == false -> RootResult.NotRooted
                    else -> RootResult.Unknown
                }
            }
        }
    }

    private fun probeAnyPackage(context: Context, packages: List<String>): Boolean {
        val pm = context.packageManager
        return packages.any { name ->
            try {
                pm.getPackageInfo(name, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    private fun probeMagiskMounts(): Boolean = try {
        File("/proc/self/mounts").readText().contains("magisk", ignoreCase = true)
    } catch (_: Exception) {
        false
    }

    private fun probeSuBinary(): Boolean = SU_PATHS.any { path ->
        try {
            File(path).exists()
        } catch (_: SecurityException) {
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
            if (code != null) {
                val major = code / 1000
                val minor = (code % 1000) / 100
                return "$major.$minor"
            }
        }
        return null
    }
}
