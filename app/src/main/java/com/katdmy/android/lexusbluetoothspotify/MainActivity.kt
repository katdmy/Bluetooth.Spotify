package com.katdmy.android.lexusbluetoothspotify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.ComponentName
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat


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
            Intent(this, NotificationListener::class.java).also { intent -> startService(intent) }
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

        connectBt?.setOnClickListener {  }
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

    class NotificationBroadcastReceiver(private val showNotificationData: (String) -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            showNotificationData(intent?.getStringExtra("SBN") ?: "")
        }
    }
}