package com.iboalali.basicrootchecker.data

import android.content.Context
import android.content.pm.PackageManager
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

sealed interface RootResult {
    data object NotRooted : RootResult
    data object Unknown : RootResult
    data class Rooted(val provider: RootProvider, val version: String?) : RootResult
}

enum class RootProvider { MAGISK, OTHER, UNKNOWN }

object RootChecker {

    private val MAGISK_PACKAGES = listOf(
        "com.topjohnwu.magisk",
        "com.topjohnwu.magisk.debug",
        "com.topjohnwu.magisk.canary",
        "com.topjohnwu.magisk.alpha",
    )

    suspend fun check(context: Context): RootResult = withContext(Dispatchers.IO) {
        val granted = Shell.isAppGrantedRoot()
        val packageHit = probeMagiskPackage(context)
        val mountHit = probeMagiskMounts()

        val result = when (granted) {
            true -> {
                val version = queryMagiskVersion()
                val isMagisk = version != null || packageHit || mountHit || probeMagiskFiles()
                RootResult.Rooted(
                    provider = if (isMagisk) RootProvider.MAGISK else RootProvider.OTHER,
                    version = version,
                )
            }

            false -> RootResult.NotRooted
            null -> RootResult.Unknown
        }
        delay(1000)
        result
    }

    private fun probeMagiskPackage(context: Context): Boolean {
        val pm = context.packageManager
        return MAGISK_PACKAGES.any { name ->
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

    private fun probeMagiskFiles(): Boolean {
        val result = Shell.cmd(
            "test -d /data/adb/magisk || test -d /sbin/.magisk || test -d /data/adb/modules"
        ).exec()
        return result.isSuccess
    }

    private fun queryMagiskVersion(): String? {
        val nameResult = Shell.cmd("magisk -V").exec()
        if (nameResult.isSuccess) {
            val name = nameResult.out.firstOrNull()?.trim().orEmpty()
            if (name.isNotEmpty()) return name
        }
        val codeResult = Shell.cmd("magisk -vV").exec()
        if (codeResult.isSuccess) {
            val raw = codeResult.out.firstOrNull()?.trim().orEmpty()
            val code = raw.substringBefore(':').toLongOrNull()
            if (code != null) {
                val major = code / 1000
                val minor = (code % 1000) / 100
                return "$major.$minor"
            }
        }
        return null
    }
}
