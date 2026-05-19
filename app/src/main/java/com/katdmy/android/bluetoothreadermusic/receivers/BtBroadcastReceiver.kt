package com.katdmy.android.bluetoothreadermusic.receivers

import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.util.BTRMDataStore
import com.katdmy.android.bluetoothreadermusic.util.Constants.USE_TTS_SF
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BtBroadcastReceiver(
    private val changeConnectionStatus: (String) -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val status = when (intent?.extras?.getInt(BluetoothProfile.EXTRA_STATE)) {
            BluetoothProfile.STATE_DISCONNECTED -> {
                changeUseTTS(context, false)
                context.getString(R.string.bt_disconnected)
            }
            BluetoothProfile.STATE_CONNECTING -> context.getString(R.string.bt_connecting)
            BluetoothProfile.STATE_CONNECTED -> {
                changeUseTTS(context, true)
                context.getString(R.string.bt_connected)
            }
            BluetoothProfile.STATE_DISCONNECTING -> context.getString(R.string.bt_disconnecting)
            else -> context.getString(R.string.bt_error)
        }
        changeConnectionStatus(status)
    }

    private fun changeUseTTS(context: Context, useTTS: Boolean) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            BTRMDataStore.saveValue(useTTS, USE_TTS_SF, context)
        }
    }
}