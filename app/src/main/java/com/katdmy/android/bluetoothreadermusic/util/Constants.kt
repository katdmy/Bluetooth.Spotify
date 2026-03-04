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
    TTS_MODE:
    0 - all apps
    1 - added apps only
    */
    val TTS_MODE = intPreferencesKey("TTS_MODE")

    /*
    VOICE_NOTIFICATION_APPS - string containing package names for apps that should be read with TTS
    */
    val VOICE_NOTIFICATION_APPS = stringPreferencesKey("VOICE_NOTIFICATION_APPS")

    /*
    RANDOM_VOICE - sets if TTS should use random voice instead of default one
    */
    val RANDOM_VOICE = booleanPreferencesKey("RANDOM_VOICE")

    /*
    SERVICE_LAST_HEARTBEAT - time when service was last alive
    */
    val SERVICE_LAST_HEARTBEAT: String = "SERVICE_LAST_HEARTBEAT"
}