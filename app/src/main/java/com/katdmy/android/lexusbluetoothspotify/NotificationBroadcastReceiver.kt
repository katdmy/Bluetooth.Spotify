package com.katdmy.android.lexusbluetoothspotify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationBroadcastReceiver(
    private val showNotificationData: (String) -> Unit?,
    private val stopTTS: () -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val command = intent?.getStringExtra("command") ?: ""
        val text = intent?.getStringExtra("text") ?: ""

        when (command) {
            "showConnectionStatus" -> showNotificationData(text)
            "onNotificationStopTTSClick" -> stopTTS()
        }
    }




}