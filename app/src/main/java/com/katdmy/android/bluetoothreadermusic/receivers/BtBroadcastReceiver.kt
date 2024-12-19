package com.katdmy.android.bluetoothreadermusic.receivers

import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.katdmy.android.bluetoothreadermusic.R

class BtBroadcastReceiver(
    private val changeUseTTS: (Boolean) -> Unit,
    private val changeConnectionStatus: (String) -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val status = when (intent?.extras?.getInt(BluetoothProfile.EXTRA_STATE)) {
            BluetoothProfile.STATE_DISCONNECTED -> {
                changeUseTTS(false)
                context.getString(R.string.bt_disconnected)
            }
            BluetoothProfile.STATE_CONNECTING -> context.getString(R.string.bt_connecting)
            BluetoothProfile.STATE_CONNECTED -> {
                changeUseTTS(true)
                context.getString(R.string.bt_connected)
            }
            BluetoothProfile.STATE_DISCONNECTING -> context.getString(R.string.bt_disconnecting)
            else -> context.getString(R.string.bt_error)
        }
        changeConnectionStatus(status)

    }
}