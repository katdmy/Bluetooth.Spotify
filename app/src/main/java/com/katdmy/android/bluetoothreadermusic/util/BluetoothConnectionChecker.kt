package com.katdmy.android.bluetoothreadermusic.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context

class BluetoothConnectionChecker(
    context: Context,
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
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.A2DP) {
                    a2dpProfile = null
                }
            }
        }, BluetoothProfile.A2DP)
    }

    private fun checkA2DPConnection() : String {
        val connectedDevices: List<BluetoothDevice>? = a2dpProfile?.connectedDevices
        if (!connectedDevices.isNullOrEmpty()) {
            close()
            return "CONNECTED"
        }
        close()
        return "DISCONNECTED"
    }

    private fun close() {
        bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, a2dpProfile)
    }

    private fun getBluetoothAdapter(context: Context): BluetoothAdapter {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter
    }

}