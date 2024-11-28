package com.katdmy.android.bluetoothreadermusic.util

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object Constants {
    /*
    USE_TTS_SF - main switch for tts work
    */
    val USE_TTS_SF = booleanPreferencesKey("USE_TTS_SF")

    /*
    SERVICE_STARTED - variable for autostart service in case of emergency stop
    */
    val SERVICE_STARTED = booleanPreferencesKey("SERVICE_STARTED")

    /*
    MUSIC_PACKAGE_NAME - package app name for selected music player
    */
    val MUSIC_PACKAGE_NAME = stringPreferencesKey("MUSIC_PACKAGE_NAME")

    /*
    TTS_MODE:
    0 - all apps
    1 - selected messengers only
    */
    val TTS_MODE = intPreferencesKey("TTS_MODE")

    /*
    ENABLED_MESSENGERS - string containing package names for chosen messengers
    */
    val ENABLED_MESSENGERS = stringPreferencesKey("ENABLED_MESSENGERS")
}