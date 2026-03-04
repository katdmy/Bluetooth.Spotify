package com.katdmy.android.bluetoothreadermusic.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

object PrefsHelper {

    private var prefs: SharedPreferences? = null

    fun getPrefs(context: Context): SharedPreferences {
        if (prefs == null)
            prefs = context.applicationContext
                .getSharedPreferences("service_state", MODE_PRIVATE)
        return prefs!!
    }
}