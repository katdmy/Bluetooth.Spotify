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
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*

class NotificationBroadcastReceiver(private val showNotificationData: (String) -> Unit) : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent?) {
        val packageName = intent?.getStringExtra("Package Name") ?: ""
        val key = intent?.getStringExtra("Key") ?: ""
        val title = intent?.getStringExtra("Title") ?: ""
        val text = intent?.getStringExtra("Text") ?: ""

        if (packageName == "ru.alarmtrade.connect") {
            showNotificationData("$packageName\n$key\n$title\n$text")
            if (key == "0|ru.alarmtrade.connect|1076889714|null|10269")
                connectBta(context)
        }

    }


    private fun connectBta(context: Context?) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            showNotificationData("Device doesn't support Bluetooth")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            showNotificationData("Please turn Bluetooth on")
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

            showNotificationData("Start BTA connecting")

            scope.launch {
                var isConnected = false
                var connectionAttempts = 0

                while (!isConnected && connectionAttempts < 20) {
                    connectionAttempts += 1
                    isConnected = connectBtaAttempt(btaBluetoothDevice!!, context)
                }
            }
        }
    }


    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun connectBtaAttempt(btaBluetoothDevice: BluetoothDevice, context: Context?) : Boolean {
        val btSocket: BluetoothSocket =
                btaBluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(BtNames.BT_DEVICE_UUID))

        return try {
            btSocket.connect()
            delay(1_000L)
            btSocket.close()
            showNotificationData("Successful connected!")

            openMusic(context)
            true
        } catch (e: IOException) {
            btSocket.close()
            showNotificationData("Error connecting attempt")
            false
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
            showNotificationData("Music player is not instaled, can't autostart it.")
        }
    }


}