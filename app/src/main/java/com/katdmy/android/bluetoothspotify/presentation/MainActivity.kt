package com.katdmy.android.bluetoothspotify.presentation

import android.app.Activity
import android.bluetooth.BluetoothProfile
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.google.android.material.switchmaterial.SwitchMaterial
import com.katdmy.android.bluetoothspotify.*
import com.katdmy.android.bluetoothspotify.Constants.useTTS_SF
import com.katdmy.android.bluetoothspotify.receivers.BtBroadcastReceiver
import com.katdmy.android.bluetoothspotify.receivers.NotificationBroadcastReceiver
import com.katdmy.android.bluetoothspotify.services.NotificationListener


class MainActivity : Activity() {

    private var stopBtn: Button? = null
    private var startBtn: Button? = null
    private var voiceSwitch: SwitchMaterial? = null
    private var btStatusTv: TextView? = null
    private var clearBtn: Button? = null
    private var tv: TextView? = null
    private var openMusicBtn: Button? = null

    private lateinit var notificationBroadcastReceiver: NotificationBroadcastReceiver
    private lateinit var btBroadcastReceiver: BtBroadcastReceiver

    private var useTTS: Boolean = false
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(applicationContext)

        notificationBroadcastReceiver = NotificationBroadcastReceiver { changeUseTTS(useTTS) }
        btBroadcastReceiver = BtBroadcastReceiver { status -> showBtStatus(status) }

        initViews()
        setUpClickListeners()
        registerReceivers()
        useTTS = sharedPreferences.getBoolean(useTTS_SF, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(notificationBroadcastReceiver)
        stopBtn = null
        startBtn = null
        voiceSwitch = null
        btStatusTv = null
        clearBtn = null
        tv = null
        openMusicBtn = null
    }


    private fun initViews() {
        stopBtn = findViewById(R.id.stop_btn)
        startBtn = findViewById(R.id.start_btn)
        voiceSwitch = findViewById(R.id.voice_sw)
        btStatusTv = findViewById(R.id.bt_status_tv)
        clearBtn = findViewById(R.id.clear_btn)
        tv = findViewById(R.id.tv)
        openMusicBtn = findViewById(R.id.open_music_btn)
    }

    private fun setUpClickListeners() {

        stopBtn?.setOnClickListener {
            packageManager.setComponentEnabledSetting(
                ComponentName(
                    this,
                    NotificationListener::class.java
                ), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
            )
        }

        startBtn?.setOnClickListener {
            Log.e(this.javaClass.simpleName, "applicationContext: $applicationContext")
            packageManager.setComponentEnabledSetting(
                ComponentName(
                    this,
                    NotificationListener::class.java
                ), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
            )
            packageManager.setComponentEnabledSetting(
                ComponentName(
                    this,
                    NotificationListener::class.java
                ), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
            )

            if (!isNotificationServiceRunning()) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        voiceSwitch?.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences.edit()
            editor.putBoolean(useTTS_SF, isChecked)
            editor.apply()

            val intent =
                Intent("com.katdmy.android.bluetoothspotify.notificationListenerService")
            intent.putExtra("command", "onVoiceUseChange")
            sendBroadcast(intent)
        }

        clearBtn?.setOnClickListener { tv?.text = "" }
        openMusicBtn?.setOnClickListener { openMusic() }
    }

    private fun isNotificationServiceRunning(): Boolean {
        val enabledNotificationListeners =
            Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabledNotificationListeners != null && enabledNotificationListeners.contains(
            packageName
        )
    }

    private fun registerReceivers() {
        if (Settings.Secure.getString(this.contentResolver, "enabled_notification_listeners")
                .contains(
                    applicationContext.packageName
                )
        ) {
            val notificationsIntentFilter = IntentFilter().apply {
                addAction("com.katdmy.android.bluetoothspotify")
            }
            registerReceiver(notificationBroadcastReceiver, notificationsIntentFilter)

            val btStatusIntentFilter = IntentFilter().apply {
                addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
            }
            registerReceiver(btBroadcastReceiver, btStatusIntentFilter)

            Intent(this, NotificationListener::class.java).also { intent -> startService(intent) }
        } else
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }

    private fun changeUseTTS(useTTS: Boolean) {
        voiceSwitch?.isChecked = useTTS
    }


    private fun openMusic() {
        val launchIntent = packageManager.getLaunchIntentForPackage("com.spotify.music")
        Log.e("openMusicActivity", launchIntent.toString())
        if (launchIntent != null)
            startActivity(launchIntent)
    }

    private fun showBtStatus(status: String) {
        BluetoothProfile.EXTRA_STATE
        btStatusTv?.text = "BT audio status: $status"
    }

}

