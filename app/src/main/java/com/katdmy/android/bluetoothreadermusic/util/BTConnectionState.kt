package com.katdmy.android.bluetoothreadermusic.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object BTConnectionState {
    private val _state = MutableStateFlow("UNKNOWN")

    val state: StateFlow<String> = _state

    fun set(message: String) {
        _state.tryEmit(message)
    }
}