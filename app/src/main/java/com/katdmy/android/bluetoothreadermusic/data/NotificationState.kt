package com.katdmy.android.bluetoothreadermusic.data

sealed class NotificationState {
    object ListenerError: NotificationState()
    object UseTTS: NotificationState()
    object NoUseTTS: NotificationState()
}