package com.katdmy.android.bluetoothreadermusic.data.models

data class GrantedPermissions(
    val readNotifications: Boolean = false,
    val postNotification: Boolean = false,
    var btStatus: Boolean = false
)