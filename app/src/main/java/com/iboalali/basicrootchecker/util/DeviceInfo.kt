package com.iboalali.basicrootchecker.util

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.util.Log
import com.iboalali.basicrootchecker.R

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

    fun getAndroidVersionName(resources: Resources): String {
        val versionNames = resources.getStringArray(R.array.VersionNames)
        return if (Build.VERSION.SDK_INT > versionNames.size) {
            "(Unreleased Android version)"
        } else {
            versionNames[Build.VERSION.SDK_INT - 1]
        }
    }
}
