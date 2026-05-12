package com.katdmy.android.bluetoothreadermusic.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.katdmy.android.bluetoothreadermusic.util.Constants.USE_TTS_SF
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BluetoothConnectionChecker(
    private val context: Context,
    callback: (String) -> Unit
) {

    private val bluetoothAdapter: BluetoothAdapter = getBluetoothAdapter(context)
    private var a2dpProfile: BluetoothProfile? = null

    init {
        bluetoothAdapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (profile == BluetoothProfile.A2DP) {
                    a2dpProfile = proxy
                    callback(checkA2DPConnection())
                    close()
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.A2DP) {
                    a2dpProfile = null
                }
            }
        }, BluetoothProfile.A2DP)
    }

    private fun getBluetoothAdapter(context: Context): BluetoothAdapter {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter
    }

    private fun checkA2DPConnection() : String {
        val connectedDevices: List<BluetoothDevice>? = a2dpProfile?.connectedDevices
        if (!connectedDevices.isNullOrEmpty()) {
            changeUseTTS(context, true)
            return "CONNECTED"
        }
        changeUseTTS(context, false)
        return "DISCONNECTED"
    }

    private fun close() {
        bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, a2dpProfile)
    }

    private fun changeUseTTS(context: Context, useTTS: Boolean) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            BTRMDataStore.saveValue(useTTS, USE_TTS_SF, context)
        }
    }

}