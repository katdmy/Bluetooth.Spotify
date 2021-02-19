package com.katdmy.android.lexusbluetoothspotify

import android.bluetooth.*
import android.content.Context

class BT(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private val profilesClass = mapOf(
            BluetoothA2dp::class.java to BluetoothProfile.A2DP,
            BluetoothHeadset::class.java to BluetoothProfile.HEADSET
    )

    fun BluetoothDevice.connect() {
        invokeAll(this, "connect", BluetoothProfile.STATE_DISCONNECTED)
    }

    fun BluetoothDevice.disconnect() {
        invokeAll(this, "disconnect", BluetoothProfile.STATE_CONNECTED)
    }

    private fun invokeAll(bluetoothDevice: BluetoothDevice, methodName: String, bondState: Int) {
        profilesClass.forEach { (`class`, profile) ->
            bluetoothAdapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceDisconnected(profile: Int) {}

                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                    val device = proxy?.getDevicesMatchingConnectionStates(intArrayOf(bondState))
                            ?.find { it.address == bluetoothDevice.address }
                            ?: return
                    val method = `class`.getDeclaredMethod(methodName, BluetoothDevice::class.java)
                    method.isAccessible = true
                    method.invoke(proxy, device)
                }
            }, profile)
        }
    }


}