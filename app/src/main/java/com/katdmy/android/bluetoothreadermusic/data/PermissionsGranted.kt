package com.katdmy.android.bluetoothreadermusic.data

data class PermissionsGranted(
    val readNotifications: Boolean = false,
    val postNotification: Boolean = false,
    var btStatus: Boolean = false
)