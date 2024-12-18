package com.katdmy.android.bluetoothreadermusic.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationBroadcastReceiver(
    private var addLogRecord: (String) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val data = intent?.getStringExtra("Data") ?: ""
        if (data != "") addLogRecord(data)
    }
}