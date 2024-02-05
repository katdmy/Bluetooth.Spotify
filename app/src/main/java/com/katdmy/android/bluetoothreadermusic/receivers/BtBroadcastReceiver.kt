package com.katdmy.android.bluetoothreadermusic.receivers

import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BtBroadcastReceiver(
    private val changeUseTTS: (Boolean) -> Unit,
    private val changeConnectionStatus: (String) -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val status = when (intent?.extras?.getInt(BluetoothProfile.EXTRA_STATE)) {
            BluetoothProfile.STATE_DISCONNECTED -> {
                changeUseTTS(false)
                "DISCONNECTED"
            }
            BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
            BluetoothProfile.STATE_CONNECTED -> {
                changeUseTTS(true)
                "CONNECTED"
            }
            BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
            else -> "error"
        }
        changeConnectionStatus(status)
    }
}