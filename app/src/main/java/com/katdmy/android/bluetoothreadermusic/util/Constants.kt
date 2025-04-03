package com.katdmy.android.bluetoothreadermusic.util

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object Constants {
    /*
    IGNORE_LACK_OF_PERMISSION - var enabling app start when user reject notifications reading permission
    */
    val IGNORE_LACK_OF_PERMISSION = booleanPreferencesKey("IGNORE_LACK_OF_PERMISSION")

    /*
    ONBOARDING_COMPLETE - if false, show onboarding screen
    */
    val ONBOARDING_COMPLETE = booleanPreferencesKey("ONBOARDING_COMPLETE")

    /*
    USE_TTS_SF - main switch for tts work
    */
    val USE_TTS_SF = booleanPreferencesKey("USE_TTS_SF")

    /*
    SERVICE_STARTED - variable disabling service work even on bluetooth connection and usetts=true
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

    /*
    RANDOM_VOICE - sets if TTS should use random voice instead of default one
    */
    val RANDOM_VOICE = booleanPreferencesKey("RANDOM_VOICE")
}