package com.iboalali.basicrootchecker;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;

/**
 * Created by Ibrahim on 17-Jul-15.
 */
public final class Utils {

    public static String getAppVersionNumber(Context context){
        PackageManager manager = context.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getAndroidName(){
        StringBuilder builder = new StringBuilder();
        Field[] fields = Build.VERSION_CODES.class.getFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            int fieldValue = -1;

            try {
                fieldValue = field.getInt(new Object());
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
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

    public static String getAndroidName(Integer SDKversion){
        StringBuilder builder = new StringBuilder();
        Field[] fields = Build.VERSION_CODES.class.getFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            int fieldValue = -1;

            try {
                fieldValue = field.getInt(new Object());
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

            if (fieldValue == SDKversion) {
                if (builder.length() != 0){
                    builder = new StringBuilder();
                }
                builder.append(fieldName);
            }
        }

        return builder.toString().replace('_', ' ');

    }

    public static String capitalizeWord(String word){
        word = word.toLowerCase();
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

}
