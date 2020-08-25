package com.iboalali.basicrootchecker;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;

/**
 * Created by Ibrahim on 17-Jul-15.
 */
final class Utils {

    static String getAppVersionNumber(Context context) {
        PackageManager manager = context.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    static String getAndroidName(Resources resources) {
        String[] versionNames = resources.getStringArray(R.array.VersionNames);

        if (Build.VERSION.SDK_INT > versionNames.length) {
            return "(Unreleased Android version)";
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return versionNames[Build.VERSION.SDK_INT - 1];
        }

        return "";
    }
}
