package com.katdmy.android.bluetoothreadermusic.util

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object Constants {
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
        APP_VOICE_SETTINGS - string containing serializable voice settings for every added app
    */
    val APP_VOICE_SETTINGS = stringPreferencesKey("APP_VOICE_SETTINGS")

    /*
    RANDOM_VOICE - sets if TTS should use random voice instead of default one
    */
    val RANDOM_VOICE = booleanPreferencesKey("RANDOM_VOICE")

    /*
    SERVICE_LAST_HEARTBEAT - time when service was last alive
    */
    val SERVICE_LAST_HEARTBEAT: String = "SERVICE_LAST_HEARTBEAT"

    /*
    TTS_VOLUME - volume for tts engine
    */
    val TTS_VOLUME = floatPreferencesKey("TTS_VOLUME")

    /*
    SHOW_LOG - should show log card on main screen or no
    */
    val SHOW_LOG = booleanPreferencesKey("SHOW_LOG")

    /*
    GLOBAL_AUDIOFOCUS_MODE - stores serializable value of global audiofocus mode
    */
    val GLOBAL_AUDIOFOCUS_MODE = stringPreferencesKey("GLOBAL_AUDIOFOCUS_MODE")

    /*
    GLOBAL_NOTIFICATION_PARTS - stores serializable value of added app voice settings
    */
    val GLOBAL_NOTIFICATION_PARTS = stringPreferencesKey("GLOBAL_NOTIFICATION_PARTS")

    /*
    READ_UPDATES - should service read notification updates or only new notifications
    */
    val READ_UPDATES = booleanPreferencesKey("READ_UPDATES")
}