package com.iboalali.basicrootchecker;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * Created by Ibrahim on 17-Jul-15.
 */
final class Utils {

    static String getAppVersionNumber(Context context){
        PackageManager manager = context.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    static String getAndroidName(Context context, Integer API_Level){
        String[] versionNames = context.getResources().getStringArray(R.array.VersionNames);
        if (API_Level > versionNames.length){
            return "(Unreleased Android version)";
        }

        // Android O Beta DP2 is still on API Level 25
        if (Build.VERSION.RELEASE.equals("O")){
            return "";
        }

        return versionNames[API_Level - 1];
    }

    private static String capitalizeWord(String word){
        word = word.toLowerCase();
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

}
