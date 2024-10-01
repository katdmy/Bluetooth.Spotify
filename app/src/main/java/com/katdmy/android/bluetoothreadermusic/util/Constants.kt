package com.katdmy.android.bluetoothreadermusic.util

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object Constants {
    val USE_TTS_SF = booleanPreferencesKey("USE_TTS_SF")
    val SERVICE_STARTED = booleanPreferencesKey("SERVICE_STARTED")
    val MUSIC_PACKAGE_NAME = stringPreferencesKey("MUSIC_PACKAGE_NAME")
}