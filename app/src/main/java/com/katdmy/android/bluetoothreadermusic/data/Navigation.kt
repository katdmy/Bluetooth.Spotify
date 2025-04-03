package com.katdmy.android.bluetoothreadermusic.data

sealed class Navigation {
    object Main: Navigation()
    object SettingsScreen: Navigation()
    object PrivacyPolicyScreeen: Navigation()
}