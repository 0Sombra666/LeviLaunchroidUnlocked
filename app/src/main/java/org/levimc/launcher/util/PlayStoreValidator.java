package org.levimc.launcher.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

public class PlayStoreValidator {
    private static final String MINECRAFT_PACKAGE_NAME = "com.mojang.minecraftpe";
    private static final String PLAY_STORE_INSTALLER = "com.android.vending";

    public static boolean isMinecraftFromPlayStore(Context context) {
        return true;
    }

    public static boolean isMinecraftInstalled(Context context) {
        return true;
    }

    public static boolean isLicenseVerified(Context context) {
        return true;
    }

}
