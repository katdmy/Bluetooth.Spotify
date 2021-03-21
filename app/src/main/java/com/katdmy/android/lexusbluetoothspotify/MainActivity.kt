package com.katdmy.android.lexusbluetoothspotify

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity(){

    private var stopBtn: Button? = null
    private var startBtn: Button? = null
    private var serviceStatusTv: TextView? = null
    private var btStatusTv: TextView? = null
    private var clearBtn: Button? = null
    private var createNotificationBtn: Button? = null
    private var tv: TextView? = null
    private var connectBtaBtn: Button? = null
    private var openMusicBtn: Button? = null

    private val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
    private val notificationBroadcastReceiver = NotificationBroadcastReceiver { text -> tv?.append(text) }
    private val btBroadcastReceiver = BtBroadcastReceiver { data -> btStatusTv?.text = data }
    private val PERMISSION_CODE = 654
    private val REQUEST_ENABLE_BT = 655
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setUpClickListeners()
        registerReceivers()

        //serviceStatusTv?.text = getString(R.string.service_status, if (isNotificationServiceEnabled()) "running" else "stopped")
        serviceStatusTv?.text = getString(R.string.service_status, if (isNotificationListenerServiceEnabled(this)) "running" else "stopped")
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)  // Menu Resource, Menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(notificationBroadcastReceiver)
        stopBtn = null
        startBtn = null
        serviceStatusTv = null
        btStatusTv = null
        clearBtn = null
        createNotificationBtn = null
        tv = null
        connectBtaBtn = null
        openMusicBtn = null
    }


    private fun initViews() {
        stopBtn = findViewById(R.id.stop_btn)
        startBtn = findViewById(R.id.start_btn)
        serviceStatusTv = findViewById(R.id.service_status_tv)
        btStatusTv = findViewById(R.id.bt_status_tv)
        clearBtn = findViewById(R.id.clear_btn)
        createNotificationBtn = findViewById(R.id.create_notification_btn)
        tv = findViewById(R.id.tv)
        connectBtaBtn = findViewById(R.id.connect_bta_btn)
        openMusicBtn = findViewById(R.id.open_music_btn)
    }

    private fun setUpClickListeners() {

        stopBtn?.setOnClickListener {
            Intent(this, NotificationListener::class.java).also { intent -> stopService(intent) }
            serviceStatusTv?.text = getString(R.string.service_status, if (isNotificationServiceEnabled()) "running" else "stopped")
            //stopForeground(ForegroundNotificationListener(), 0)
        }

        startBtn?.setOnClickListener {
            Intent(this, NotificationListener::class.java).also { intent -> startService(intent) }
            serviceStatusTv?.text = getString(R.string.service_status, if (isNotificationServiceEnabled()) "running" else "stopped")
        }

        clearBtn?.setOnClickListener { tv?.text = "" }

        /*createNotificationBtn?.setOnClickListener {
            val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val mBuilder =
                    NotificationCompat.Builder(this@MainActivity, default_notification_channel_id)
            mBuilder.setContentTitle("My Notification")
            mBuilder.setContentText("Notification Listener Service Example")
            mBuilder.setTicker("Notification Listener Service Example")
            mBuilder.setSmallIcon(R.drawable.ic_baseline_notifications_active_24)
            mBuilder.setAutoCancel(true)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "NOTIFICATION_CHANNEL_NAME",
                    importance
            )
            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID)
            mNotificationManager.createNotificationChannel(notificationChannel)
            mNotificationManager.notify(System.currentTimeMillis().toInt(), mBuilder.build())
        }*/

        connectBtaBtn?.setOnClickListener {
            connectBta()
        }

        openMusicBtn?.setOnClickListener {
            openMusic()
        }
    }

    private fun registerReceivers() {
        if (Settings.Secure.getString(this.contentResolver, "enabled_notification_listeners").contains(
                        applicationContext.packageName
                )) {
            val notificationsIntentFilter = IntentFilter().apply {
                addAction("com.katdmy.android.lexusbluetoothspotify")
            }
            registerReceiver(notificationBroadcastReceiver, notificationsIntentFilter)

            val btStatusIntentFilter = IntentFilter().apply {
                addAction("android.bluetooth.device.action.ACL_CONNECTED")
                addAction("android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED")
                addAction("android.bluetooth.device.action.ACL_DISCONNECTED")
            }
            registerReceiver(btBroadcastReceiver, btStatusIntentFilter)

            //Intent(this, NotificationListener::class.java).also { intent -> startService(intent) }
        } else
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat: String = Settings.Secure.getString(
                contentResolver,
                ENABLED_NOTIFICATION_LISTENERS
        )
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (i in names.indices) {
                val cn = ComponentName.unflattenFromString(names[i])
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.packageName)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun isNotificationListenerServiceEnabled(context: Context): Boolean {
        val packageNames = NotificationManagerCompat.getEnabledListenerPackages(context)
        return packageNames.contains(context.packageName)
    }

    private fun connectBta() {
        if (ContextCompat.checkSelfPermission(baseContext,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    PERMISSION_CODE)
        }

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        tv?.text = ""
        var btaBluetoothDevice: BluetoothDevice? = null
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        pairedDevices?.forEach { device ->
            if (device.name == BtNames.BT_DEVICE_NAME) {
                btaBluetoothDevice = device // BTA bluetooth device
            }
        }
        if (btaBluetoothDevice != null) {
            tv?.append("BTA address: ${btaBluetoothDevice!!.address}\n")
            bluetoothAdapter.cancelDiscovery()

            scope.launch {
                var isConnected = false
                var connectionAttempts = 0

                while (!isConnected && connectionAttempts < 20) {
                    connectionAttempts += 1
                    isConnected = connectBtaAttempt(btaBluetoothDevice!!)
                }
            }

        } else {
            tv?.append("BTA is not paired")
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun connectBtaAttempt(btaBluetoothDevice: BluetoothDevice) : Boolean {
        val btSocket: BluetoothSocket =
                btaBluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(BtNames.BT_DEVICE_UUID))

        return try {
            btSocket.connect()
            withContext(Dispatchers.Main) { tv?.append("\nBTA is connected!") }
            delay(1_000L)
            btSocket.close()

            openMusic()
            true
        } catch (e: IOException) {
            withContext(Dispatchers.Main) { tv?.append("\nBTA is not connected.") }
            btSocket.close()
            false
        }
    }

    private fun openMusic() {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("spotify:app")
            intent.putExtra(
                    Intent.EXTRA_REFERRER,
                    Uri.parse("android-app://" + this.packageName)
            )
            this.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            tv?.append("\nMusic player is not instaled, can't autostart it.")
        }
    }

}

