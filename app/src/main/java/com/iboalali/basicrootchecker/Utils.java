package com.iboalali.basicrootchecker;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.lang.reflect.Field;

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

    static String getAndroidName(){
        StringBuilder builder = new StringBuilder();
        Field[] fields = Build.VERSION_CODES.class.getFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            int fieldValue = -1;

            try {
                fieldValue = field.getInt(new Object());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            if (fieldValue == Build.VERSION.SDK_INT) {
                if (builder.length() != 0){
                    builder = new StringBuilder();
                }
                builder.append(fieldName);
            }
        }

        return capitalizeWord(builder.toString().replace('_', ' '));

    }

    public static String getAndroidName(Context context, Integer API_Level){
        String[] versionNames = context.getResources().getStringArray(R.array.VersionNames);
        if (API_Level > versionNames.length){
            return "(Unreleased Android version)";
        }

        return versionNames[API_Level - 1];
    }

    private static String capitalizeWord(String word){
        word = word.toLowerCase();
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

}
