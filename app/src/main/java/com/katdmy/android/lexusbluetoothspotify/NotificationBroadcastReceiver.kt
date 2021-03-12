package com.katdmy.android.lexusbluetoothspotify

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*

class NotificationBroadcastReceiver(private val showNotificationData: (String) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        showNotificationData(intent?.getStringExtra("SBN") ?: "")

        if (intent?.getStringExtra("Package Name")?.equals("ru.alarmtrade.connect") == true
                && intent.getStringExtra("Key")?.equals("0|ru.alarmtrade.connect|1076889714|null|10269") == true)
                    connectBta(context)
    }


    private fun connectBta(context: Context?) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(context, "Please turn Bluetooth on", Toast.LENGTH_LONG).show()
            return
        }

        var btaBluetoothDevice: BluetoothDevice? = null
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        pairedDevices?.forEach { device ->
            if (device.name == BtNames.BT_DEVICE_NAME) {
                btaBluetoothDevice = device // BTA bluetooth device
            }
        }
        if (btaBluetoothDevice != null) {
            bluetoothAdapter.cancelDiscovery()

            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    val btSocket: BluetoothSocket =
                            btaBluetoothDevice!!.createRfcommSocketToServiceRecord(UUID.fromString(BtNames.BT_DEVICE_UUID))

                    var isConnected = false
                    var connectionAttempts = 0
                    while (!isConnected && connectionAttempts < 20) {
                        connectionAttempts += 1
                        try {
                            btSocket.connect()
                            delay(1_000L)
                            btSocket.close()
                            isConnected = true

                            openMusic(context)
                        } catch (e: IOException) {
                            btSocket.close()
                        }
                    }
                }
            }

        }
    }

    private fun openMusic(context: Context?) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("spotify:app")
            intent.putExtra(
                    Intent.EXTRA_REFERRER,
                    Uri.parse("android-app://com.katdmy.android.lexusbluetoothspotify")
            )
            context?.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(context?.javaClass?.simpleName, "Music player is not instaled, can't autostart it.")
            Toast.makeText(context, "Music player is not instaled, can't autostart it.", Toast.LENGTH_LONG).show()
        }
    }
}