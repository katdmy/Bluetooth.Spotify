package com.katdmy.android.bluetoothreadermusic.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLog {

    private const val MAX_SIZE = 100

    private val _messages = MutableStateFlow<List<String>>(emptyList())

    val messages: StateFlow<List<String>> = _messages

    fun add(message: String) {
        val time =
            SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

        val newMessage = "[$time] $message"

        _messages.value =
            (_messages.value + newMessage)
                .takeLast(MAX_SIZE)
    }

    fun clear() {
        _messages.value = emptyList()
    }
}