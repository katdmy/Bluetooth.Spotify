package com.katdmy.android.bluetoothreadermusic.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.google.android.material.switchmaterial.SwitchMaterial
import com.katdmy.android.bluetoothreadermusic.Constants.MUSIC_PACKAGE_NAME
import com.katdmy.android.bluetoothreadermusic.Constants.USE_TTS_SF
import com.katdmy.android.bluetoothreadermusic.receivers.BtBroadcastReceiver
import com.katdmy.android.bluetoothreadermusic.receivers.NotificationBroadcastReceiver
import com.katdmy.android.bluetoothreadermusic.services.NotificationListener
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.musicApps.MusicApp


class MainActivity : ComponentActivity(), AdapterView.OnItemSelectedListener {

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
    private var currentMusicApp: MusicApp? = null
    private var useTTS: Boolean = false
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var spEditor: SharedPreferences.Editor
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        spEditor = sharedPreferences.edit()

        notificationBroadcastReceiver = NotificationBroadcastReceiver { changeUseTTS(useTTS) }
        btBroadcastReceiver = BtBroadcastReceiver { status -> showBtStatus(status) }

        initViews()
        initMusicApps()
        setUpClickListeners()
        requestPermissionLauncher = registerForActivityResult(RequestMultiplePermissions()) { permissionAndGrant ->
            if (permissionAndGrant.values.contains(false)) {
                permissionAndGrant.forEach { (name: String, isGranted: Boolean) -> if (!isGranted) showNoPermissionDialog(name) }
            }
        }
        registerReceivers()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermissions()
        }
        useTTS = sharedPreferences.getBoolean(USE_TTS_SF, false)
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
                val ai = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getApplicationInfo(
                        app,
                        PackageManager.ApplicationInfoFlags.of(0)
                    )
                } else {
                    packageManager.getApplicationInfo(app, 0)
                }
                val name = packageManager.getApplicationLabel(ai).toString()
                val lauchIntent = packageManager.getLaunchIntentForPackage(app)
                val icon = packageManager.getApplicationIcon(app)
                installedMusicApps.add(MusicApp(app, lauchIntent, name, icon))
            }
        }
        Log.e("InstalledMusicApps", installedMusicApps.toString())

        val adapter =
            ArrayAdapter(this, R.layout.music_app_list_item, installedMusicApps.map { it.name })
        musicAppSpinner?.adapter = adapter
        musicAppSpinner?.onItemSelectedListener = this

        val currentMusicAppName = sharedPreferences.getString(MUSIC_PACKAGE_NAME, "")
        if (currentMusicAppName != "") {
            currentMusicApp =
                installedMusicApps.firstOrNull { it.packageName == currentMusicAppName }
            musicAppSpinner?.setSelection(installedMusicApps.indexOf(currentMusicApp))
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                packageManager.getPackageInfo(packageName, 0)
            }
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
            editor.putBoolean(USE_TTS_SF, isChecked)
            editor.apply()

            val intent =
                Intent("com.katdmy.android.bluetoothreadermusic.notificationListenerService")
            intent.putExtra("command", "onVoiceUseChange")
            sendBroadcast(intent)
        }

        clearBtn?.setOnClickListener { tv?.text = "" }
        openMusicBtn?.setOnClickListener { startActivity(currentMusicApp?.launchIntent) }
    }

    private fun isNotificationServiceRunning(): Boolean {
        val enabledNotificationListeners =
            Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabledNotificationListeners != null && enabledNotificationListeners.contains(
            packageName
        )
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceivers() {
        if (Settings.Secure.getString(this.contentResolver, "enabled_notification_listeners")
                .contains(applicationContext.packageName)
        ) {
            val notificationsIntentFilter = IntentFilter().apply {
                addAction("com.katdmy.android.bluetoothreadermusic")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(notificationBroadcastReceiver, notificationsIntentFilter,
                    RECEIVER_EXPORTED)
            } else {
                registerReceiver(notificationBroadcastReceiver, notificationsIntentFilter)
            }

            val btStatusIntentFilter = IntentFilter().apply {
                addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(btBroadcastReceiver, btStatusIntentFilter,
                    RECEIVER_EXPORTED)
            } else {
                registerReceiver(btBroadcastReceiver, btStatusIntentFilter)
            }

            Intent(this, NotificationListener::class.java).also { intent -> startService(intent) }
        } else
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkPermissions() {
        when {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED -> {
                    //tv?.text = "All permissions granted.\n" + tv?.text
                }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.BLUETOOTH_CONNECT) -> {
                // explain to the user why your app requires this permission and what features are disabled if it's declined
                // include "cancel" button that lets the user continue without granting the permission
                showRequestPermissionDialog()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.POST_NOTIFICATIONS) -> {
                // explain to the user why your app requires this permission and what features are disabled if it's declined
                // include "cancel" button that lets the user continue without granting the permission
                showRequestPermissionDialog()
            }
            else -> {
                // directly ask for the permission, registered ActivityResultCallback gets the result
                requestPermissionLauncher.launch(arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.BLUETOOTH_CONNECT
                ))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun showRequestPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Необходимы разрешения")
            .setMessage("Для работы приложения требуются разрешение на показ уведомлений (для бесперебойной работы сервиса в фоне) и разрешение на чтение состояния подключения Bluetooth (для автоматического включения/выключения функции чтения сообщений)")
            .setPositiveButton("Разрешить") { _, _ -> requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.BLUETOOTH_CONNECT
            )) }
            .setNegativeButton("Запретить") { _, _ -> }
            .create()
            .show()
    }

    private fun showNoPermissionDialog(name: String) {
        AlertDialog.Builder(this)
            .setTitle("Отсутствует разрешение name")
            //.setMessage("Сервис, гарантирующий работу приложения в фоне, может быть закрыт системой при нехватке памяти.")
            .setMessage("Отсутвует необходимое для работы приложения разрешение:\n$name")
            .create()
            .show()
    }

    private fun changeUseTTS(useTTS: Boolean) {
        voiceSwitch?.isChecked = useTTS
    }

    private fun showBtStatus(status: String) {
        btStatusTv?.text = getString(R.string.bt_status, status)
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
        val currentPlayer = parent.getItemAtPosition(pos)
        currentMusicApp = installedMusicApps.firstOrNull { it.name == currentPlayer }
        musicAppImageView?.setImageDrawable(currentMusicApp?.icon)
        spEditor.putString(MUSIC_PACKAGE_NAME, currentMusicApp?.packageName).apply()
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        currentMusicApp = null
    }

}

