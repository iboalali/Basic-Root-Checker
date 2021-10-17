package com.iboalali.basicrootchecker;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    @SuppressWarnings("EqualsReplaceableByObjectsCall")
    static boolean equals(@Nullable Object a, @Nullable Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    static void setSystemWindowLightMode(@NonNull Activity activity, boolean on) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            View view = activity.getWindow().getDecorView();
            int flags;

            if (on) {
                flags = view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags = view.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }

            view.setSystemUiVisibility(flags);
        }
    }
}
