package com.iboalali.basicrootchecker.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

object DeviceInfo {

    fun getAppVersionName(context: Context): String {
        return try {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("DeviceInfo", "getAppVersionName: ", e)
            ""
        }
    }

    fun getAndroidVersionName(): String {
        val release = Build.VERSION.RELEASE
        val codename = when (Build.VERSION.SDK_INT) {
            23 -> "Marshmallow"
            24, 25 -> "Nougat"
            26, 27 -> "Oreo"
            28 -> "Pie"
            else -> null
        }
        return if (codename != null) "$release $codename" else release
    }
}
