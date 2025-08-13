package com.example.cyberqrscan.ui.settings;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
public class AppInfo {
    private static String versionName;
    private static Context context;
    public static void init(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionName = pInfo.versionName;
            AppInfo.context = context;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "Unknown";
        }
    }

    public static Context getContext(){
        return context;
    }

    // Getter for version name
    public static String getVersionName() {
        return versionName;
    }
}
