package com.katdmy.android.bluetoothspotify

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BtBroadcastReceiver(private val changeConnectionStatus: (Boolean) -> Unit) :
    BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> changeConnectionStatus(true)
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> changeConnectionStatus(false)
        }
    }
}