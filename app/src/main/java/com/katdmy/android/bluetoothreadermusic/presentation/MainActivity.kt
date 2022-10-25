package com.katdmy.android.bluetoothreadermusic.presentation

import android.app.Activity
import android.bluetooth.BluetoothProfile
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.preference.PreferenceManager
import com.google.android.material.switchmaterial.SwitchMaterial
import com.katdmy.android.bluetoothreadermusic.Constants.useTTS_SF
import com.katdmy.android.bluetoothreadermusic.receivers.BtBroadcastReceiver
import com.katdmy.android.bluetoothreadermusic.receivers.NotificationBroadcastReceiver
import com.katdmy.android.bluetoothreadermusic.services.NotificationListener
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.musicApps.MusicApp


class MainActivity : Activity(), AdapterView.OnItemSelectedListener {

    private var stopBtn: Button? = null
    private var startBtn: Button? = null
    private var voiceSwitch: SwitchMaterial? = null
    private var btStatusTv: TextView? = null
    private var clearBtn: Button? = null
    private var tv: TextView? = null
    private var musicAppSpinner: Spinner? = null
    private var musicAppImageView: ImageView? = null
    private var openMusicBtn: Button? = null

    private lateinit var notificationBroadcastReceiver: NotificationBroadcastReceiver
    private lateinit var btBroadcastReceiver: BtBroadcastReceiver

    private val installedMusicApps = ArrayList<MusicApp>()
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
        initMusicApps()
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
        musicAppSpinner = null
        musicAppImageView = null
        openMusicBtn = null
    }


    private fun initViews() {
        stopBtn = findViewById(R.id.stop_btn)
        startBtn = findViewById(R.id.start_btn)
        voiceSwitch = findViewById(R.id.voice_sw)
        btStatusTv = findViewById(R.id.bt_status_tv)
        clearBtn = findViewById(R.id.clear_btn)
        tv = findViewById(R.id.tv)
        musicAppSpinner = findViewById(R.id.music_app_spinner)
        musicAppImageView = findViewById(R.id.music_app_iv)
        openMusicBtn = findViewById(R.id.open_music_btn)
    }

    private fun initMusicApps() {
        val musicAppList = listOf(
            "ru.yandex.music",
            "com.spotify.music",
            "com.google.android.apps.youtube.music",
            "deezer.android.app",
            "com.apple.android.music"
        )
        for (app in musicAppList) {
            if (isAppInstalled(app)) {
                val ai = packageManager.getApplicationInfo(app, 0)
                val name = packageManager.getApplicationLabel(ai).toString()
                val lauchIntent = packageManager.getLaunchIntentForPackage(app)
                val icon = packageManager.getApplicationIcon(app)
                installedMusicApps.add(MusicApp(name, lauchIntent, icon))
            }
        }
        Log.e("InstalledMusicApps", installedMusicApps.toString())

        val adapter =
            ArrayAdapter(this, R.layout.music_app_list_item, installedMusicApps.map { it.name })
        musicAppSpinner?.adapter = adapter
        musicAppSpinner?.onItemSelectedListener = this
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
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
            //Log.e(this.javaClass.simpleName, "applicationContext: $applicationContext")
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
                Intent("com.katdmy.android.bluetoothreadermusic.notificationListenerService")
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
                addAction("com.katdmy.android.bluetoothreadermusic")
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
        val launchIntent = packageManager.getLaunchIntentForPackage("ru.yandex.music")
        Log.e("openMusicActivity", launchIntent.toString())
        if (launchIntent != null)
            startActivity(launchIntent)
    }

    private fun showBtStatus(status: String) {
        BluetoothProfile.EXTRA_STATE
        btStatusTv?.text = "BT audio status: $status"
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
        val currentPlayer = parent?.getItemAtPosition(pos)
        val currentApp = installedMusicApps.firstOrNull { it.name == currentPlayer }
        musicAppImageView?.setImageDrawable(currentApp?.icon)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

}

