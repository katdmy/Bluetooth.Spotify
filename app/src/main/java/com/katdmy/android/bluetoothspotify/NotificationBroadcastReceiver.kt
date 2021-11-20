package com.katdmy.android.bluetoothspotify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationBroadcastReceiver(
    private val showNotificationData: (String) -> Unit,
    private val stopTTS: () -> Unit,
    private val startTTS: () -> Unit,
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val packageName = intent?.getStringExtra("Package Name") ?: ""
        val key = intent?.getStringExtra("Key") ?: ""
        val title = intent?.getStringExtra("Title") ?: ""
        val text = intent?.getStringExtra("Text") ?: ""
        val command = intent?.getStringExtra("command") ?: ""

        if (packageName == "ru.alarmtrade.connect") {
            if ((context?.applicationContext as MyApplication).isAppForeground())
                showNotificationData("$packageName\n$key\n$title\n$text")
        } else if (command == "onNotificationStopTTSClick") {
            stopTTS()
        } else if (command == "onNotificationStartTTSClick") {
            startTTS()
        }
    }
}