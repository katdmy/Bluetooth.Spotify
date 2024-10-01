package com.katdmy.android.bluetoothreadermusic.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationBroadcastReceiver(
    private var addLogRecord: (String) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        val packageName = intent?.getStringExtra("Package Name") ?: ""
        val key = intent?.getStringExtra("Key") ?: ""
        val title = intent?.getStringExtra("Title") ?: ""
        val text = intent?.getStringExtra("Text") ?: ""
        val command = intent?.getStringExtra("command") ?: ""

        val data = intent?.getStringExtra("Data") ?: ""
        if (data != "") addLogRecord(data)

    }
}