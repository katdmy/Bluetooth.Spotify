package com.katdmy.android.lexusbluetoothspotify

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BtBroadcastReceiver(private val showBtStatus: (String) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            BluetoothDevice.ACTION_FOUND -> showBtStatus("BT device found")
            BluetoothDevice.ACTION_ACL_CONNECTED -> showBtStatus("BT acl connected")
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> showBtStatus("BT discovery finished")
            BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> showBtStatus("Disconnect requested")
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> showBtStatus("BT disconnected")
            "HEADSET_INTERFACE_CONNECTED" -> showBtStatus("\uD83C\uDFA7 CONNECTED!")
        }
    }
}