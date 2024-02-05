package com.katdmy.android.bluetoothreadermusic.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationBroadcastReceiver(
    private var changeUseTTS: (Boolean) -> Unit,
    private var addLogRecord: (String) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        val packageName = intent?.getStringExtra("Package Name") ?: ""
        val key = intent?.getStringExtra("Key") ?: ""
        val title = intent?.getStringExtra("Title") ?: ""
        val text = intent?.getStringExtra("Text") ?: ""
        val command = intent?.getStringExtra("command") ?: ""

        if (command == "onNotificationChangeTTSClick") {
            val newUseTTS = intent?.getBooleanExtra("useTTS", false) ?: false
            changeUseTTS(newUseTTS)
        }

        val data = intent?.getStringExtra("Data") ?: ""
        if (data != "") addLogRecord(data)

    }
}