package com.katdmy.android.bluetoothspotify

import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {

    private var stopBtn: Button? = null
    private var startBtn: Button? = null
    private var voiceSwitch: SwitchMaterial? = null
    private var btStatusTv: TextView? = null
    private var clearBtn: Button? = null
    private var tv: TextView? = null
    private var openMusicBtn: Button? = null

    private val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
    private val notificationBroadcastReceiver =
        NotificationBroadcastReceiver(
            { text: String -> tv?.append("\n$text") },
            { voiceSwitch?.isChecked = false },
            { voiceSwitch?.isChecked = true }
        )

    //private val btBroadcastReceiver = BtBroadcastReceiver { data -> btStatusTv?.text = data }
    private val PERMISSION_CODE = 654
    private val REQUEST_ENABLE_BT = 655
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(applicationContext)

        initViews()
        setUpClickListeners()
        registerReceivers()
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
        voiceSwitch!!.isChecked = sharedPreferences.getBoolean(BtNames.useTTS_SF, false)
    }

    private fun setUpClickListeners() {

        stopBtn?.setOnClickListener {
            packageManager.setComponentEnabledSetting(ComponentName(this, NotificationListener::class.java), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
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
        }

        voiceSwitch?.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences.edit()
            editor.putBoolean(BtNames.useTTS_SF, isChecked)
            editor.apply()

            val intent =
                Intent("com.katdmy.android.bluetoothspotify.notificationListenerService")
            intent.putExtra("command", "onVoiceUseChange")
            sendBroadcast(intent)
        }

        clearBtn?.setOnClickListener { tv?.text = "" }
        openMusicBtn?.setOnClickListener { openMusic() }
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

            /*val btStatusIntentFilter = IntentFilter().apply {
                addAction("android.bluetooth.device.action.ACL_CONNECTED")
                addAction("android.bluetooth.device.action.ACL_DISCONNECTED")
            }
            registerReceiver(btBroadcastReceiver, btStatusIntentFilter) */

            Intent(this, NotificationListener::class.java).also { intent -> startService(intent) }
        } else
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
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

