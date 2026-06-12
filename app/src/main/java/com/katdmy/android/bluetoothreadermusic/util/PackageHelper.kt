package com.katdmy.android.bluetoothreadermusic.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build

object PackageHelper {

    fun getAppName(context: Context, packageName: String): String? {
        val appContext = context.applicationContext
        val packageManager = appContext.packageManager

        return try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                packageManager.getApplicationInfo(packageName, 0)
            }
            packageManager.getApplicationLabel(appInfo).toString()

        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun getAppIcon(context: Context, packageName: String): Drawable {
        val appContext = context.applicationContext
        val packageManager = appContext.packageManager

        return packageManager.getApplicationIcon(packageName)
    }
}