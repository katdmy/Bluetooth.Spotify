package com.katdmy.android.bluetoothreadermusic.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ServiceHealthBus {

    private val _heartbeatFlow = MutableSharedFlow<Long>(replay = 1, extraBufferCapacity = 1)

    val heartbeatFlow = _heartbeatFlow.asSharedFlow()

    fun emitHeartbeat() {
        _heartbeatFlow.tryEmit(System.currentTimeMillis())
    }
}