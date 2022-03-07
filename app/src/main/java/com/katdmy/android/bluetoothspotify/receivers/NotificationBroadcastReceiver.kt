package com.katdmy.android.bluetoothspotify.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationBroadcastReceiver(
    private var changeUseTTS: (Boolean) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val packageName = intent?.getStringExtra("Package Name") ?: ""
        val key = intent?.getStringExtra("Key") ?: ""
        val title = intent?.getStringExtra("Title") ?: ""
        val text = intent?.getStringExtra("Text") ?: ""
        val command = intent?.getStringExtra("command") ?: ""

        if (command == "onNotificationStopTTSClick") {
            changeUseTTS(false)
        } else if (command == "onNotificationStartTTSClick") {
            changeUseTTS(true)
        }
    }
}