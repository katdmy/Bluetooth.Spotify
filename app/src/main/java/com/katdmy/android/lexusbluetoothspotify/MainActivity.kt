package com.katdmy.android.lexusbluetoothspotify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.*
import android.content.*
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity(){

    private var stopBtn: Button? = null
    private var startBtn: Button? = null
    private var statusTv: TextView? = null
    private var clearBtn: Button? = null
    private var createNotificationBtn: Button? = null
    private var tv: TextView? = null
    private var connectBt: Button? = null

    private val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
    val NOTIFICATION_CHANNEL_ID = "10001"
    private val default_notification_channel_id = "default"
    private val notificationBroadcastReceiver = NotificationBroadcastReceiver {
            text -> tv?.append("$text \n\n") }
    private val PERMISSION_CODE = 654
    private val REQUEST_ENABLE_BT = 655

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setUpClickListeners()

        statusTv?.text = getString(R.string.service_status, if (isNotificationServiceEnabled()) "running" else "stopped")

        if (Settings.Secure.getString(this.contentResolver, "enabled_notification_listeners").contains(
                applicationContext.packageName
            )) {
            val intentFilter = IntentFilter().apply { addAction("com.katdmy.android.lexusbluetoothspotify") }
            registerReceiver(notificationBroadcastReceiver, intentFilter)
//            Intent(this, NotificationListener::class.java).also { intent -> startService(intent) }
        } else
            applicationContext.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))

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
        statusTv = null
        clearBtn = null
        createNotificationBtn = null
        tv = null
        connectBt = null
    }


    private fun initViews() {
        stopBtn = findViewById(R.id.stop_btn)
        startBtn = findViewById(R.id.start_btn)
        statusTv = findViewById(R.id.status_tv)
        clearBtn = findViewById(R.id.clear_btn)
        createNotificationBtn = findViewById(R.id.create_notification_btn)
        tv = findViewById(R.id.tv)
        connectBt = findViewById(R.id.connect_bt_btn)
    }

    private fun setUpClickListeners() {

        stopBtn?.setOnClickListener {
            Intent(this, NotificationListener::class.java).also { intent -> stopService(intent) }
            statusTv?.text = getString(R.string.service_status, if (isNotificationServiceEnabled()) "running" else "stopped")
        }

        startBtn?.setOnClickListener {
            Intent(this, NotificationListener::class.java).also { intent -> startService(intent) }
            statusTv?.text = getString(R.string.service_status, if (isNotificationServiceEnabled()) "running" else "stopped")
        }

        clearBtn?.setOnClickListener { tv?.text = "" }

        createNotificationBtn?.setOnClickListener {
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
        }

        connectBt?.setOnClickListener {
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
                return@setOnClickListener
            }

            if (!bluetoothAdapter?.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }

            tv?.text = ""
            var btaBluetoothDevice : BluetoothDevice? = null
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
            pairedDevices?.forEach { device ->
                if (device.name == "BTA") {
                    btaBluetoothDevice = device // BTA bluetooth device
                }
            }
            if (btaBluetoothDevice != null) {
                tv?.append("BTA address: ${btaBluetoothDevice!!.address}\n")
                btaBluetoothDevice!!.uuids.forEach { parcelUuid ->
                    run {
                        val btSocket: BluetoothSocket =
                            btaBluetoothDevice!!.createRfcommSocketToServiceRecord(parcelUuid.uuid)

                        try {
                            btSocket.connect()
                        } catch (e: IOException) {
                            btSocket.close()
                            tv?.append("uuid: ${parcelUuid.uuid} not connected with error :$e")
                        } finally {
                            tv?.append("uuid: ${parcelUuid.uuid}\n connected!")
                        }

                    }
                }

            } else {} // BTA is not paired


            /*var bluetoothHeadset: BluetoothHeadset? = null
            val profileListener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    if (profile == BluetoothProfile.HEADSET) {
                        bluetoothHeadset = proxy as BluetoothHeadset
                    }
                }
                override fun onServiceDisconnected(profile: Int) {
                    if (profile == BluetoothProfile.HEADSET) {
                        bluetoothHeadset = null
                    }
                }
            }
            bluetoothAdapter?.getProfileProxy(applicationContext, profileListener, BluetoothProfile.HEADSET)    // Establish connection to the proxy.

// ... call functions on bluetoothHeadset
            //bluetoothHeadset.

            bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset) // Close proxy connection after use. */
        }
    }

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (resultCode == RESULT_OK) {
                    tv?.text = ""
                    val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
                    pairedDevices?.forEach { device ->
                        val deviceName = device.name
                        val deviceHardwareAddress = device.address // MAC address
                        tv?.append("name: $deviceName, address: $deviceHardwareAddress")
                    }
                } else return
            }
            else -> return
        }
    }*/

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

    class NotificationBroadcastReceiver(private val showNotificationData: (String) -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            showNotificationData(intent?.getStringExtra("SBN") ?: "")
        }
    }
}