package com.katdmy.android.bluetoothreadermusic.util

import android.content.Context
import com.katdmy.android.bluetoothreadermusic.util.Constants.LOG_ENABLED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLog {

    private const val MAX_SIZE = 100

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages

    fun add(context: Context, message: String) {
        val scope = CoroutineScope(SupervisorJob())
        scope.launch {
            if (BTRMDataStore.getValue(LOG_ENABLED, context) == true) {
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                val newMessage = "[$time] $message"

                _messages.value = (_messages.value + newMessage).takeLast(MAX_SIZE)
            }
        }
    }

    fun clear() {
        _messages.value = emptyList()
    }
}