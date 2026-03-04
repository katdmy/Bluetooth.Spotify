package com.katdmy.android.bluetoothreadermusic.ui.views

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.util.Constants.USE_TTS_SF
import com.katdmy.android.bluetoothreadermusic.data.models.InstalledApp
import com.katdmy.android.bluetoothreadermusic.receivers.BtBroadcastReceiver
import com.katdmy.android.bluetoothreadermusic.receivers.NotificationBroadcastReceiver
import com.katdmy.android.bluetoothreadermusic.services.StatusService
import com.katdmy.android.bluetoothreadermusic.ui.theme.BtReaderMusicTheme
import com.katdmy.android.bluetoothreadermusic.ui.vm.MainViewModel
import com.katdmy.android.bluetoothreadermusic.util.BTRMDataStore
import com.katdmy.android.bluetoothreadermusic.util.BluetoothConnectionChecker
import com.katdmy.android.bluetoothreadermusic.util.StringListHelper.getList
import com.katdmy.android.bluetoothreadermusic.util.StringListHelper.getString
import com.katdmy.android.bluetoothreadermusic.util.Constants.ONBOARDING_COMPLETE
import com.katdmy.android.bluetoothreadermusic.util.Constants.RANDOM_VOICE
import com.katdmy.android.bluetoothreadermusic.util.Constants.TTS_MODE
import kotlinx.coroutines.launch
import java.util.Locale
import com.katdmy.android.bluetoothreadermusic.util.Constants.VOICE_NOTIFICATION_APPS
import kotlinx.coroutines.flow.map

class ComposeActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel>()
    private lateinit var notificationBroadcastReceiver: NotificationBroadcastReceiver
    private lateinit var btBroadcastReceiver: BtBroadcastReceiver
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var tts: TextToSpeech
    private lateinit var audioManager: AudioManager
    private lateinit var focusRequest: AudioFocusRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        notificationBroadcastReceiver = NotificationBroadcastReceiver(
            addLogRecord = viewModel::onAddLogMessage
        )
        btBroadcastReceiver = BtBroadcastReceiver(
            changeUseTTS = { useTTS: Boolean ->
                lifecycleScope.launch {
                    BTRMDataStore.saveValue(useTTS, USE_TTS_SF, this@ComposeActivity)
                }
            },
            changeConnectionStatus = viewModel::onChangeBtStatus
        )

        initTTS()

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { permissionAndGrant ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (permissionAndGrant.keys.contains(Manifest.permission.POST_NOTIFICATIONS)) {
                        viewModel.onSetPostNotificationPermission(
                            permissionAndGrant[Manifest.permission.POST_NOTIFICATIONS] == true
                        )
                    }
                }
            if (permissionAndGrant.keys.contains(Manifest.permission.BLUETOOTH_CONNECT)) {
                viewModel.onSetBTStatusPermission(permissionAndGrant[Manifest.permission.BLUETOOTH_CONNECT] == true)
                if (permissionAndGrant[Manifest.permission.BLUETOOTH_CONNECT] == true)
                    getInitialBluetoothStatus(viewModel::onChangeBtStatus)
            }
        }

        registerReceivers()
        val intent = Intent(this@ComposeActivity, StatusService::class.java)
        startForegroundService(intent)

        setContent {
            val isOnboardingComplete by BTRMDataStore.getValueFlow(ONBOARDING_COMPLETE, this).collectAsState(initial = false)

            BtReaderMusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isOnboardingComplete == true)
                        BTReaderApp(
                            viewModel,
                            onGetInstalledLaunchableApps = ::getInstalledLaunchableApps,
                            onClickReadTestText = ::onClickReadTestText,
                            onClickStopReading = ::onClickStopReading,
                            onClickDeleteApp = ::onClickDeleteApp,
                            onAddApps = ::onAddApps,
                            restartNotificationListener = ::restartNotificationListener,
                            onChangeUseTTS = ::onChangeUseTTS,
                            onSetTtsMode = ::onSetTtsMode,
                            onSetRandomVoice = ::onSetRandomVoice,
                            onClickRequestReadNotificationsPermission = ::onRequestReadNotificationsPermission,
                            onClickRequestPostNotificationPermission = ::onRequestShowNotificationPermission,
                            onClickRequestBtPermission = ::onRequestBtPermission,
                            onClickAbandonAudiofocus = ::onClickAbandonAudiofocus
                        )
                    else
                        OnboardingScreen(
                            viewModel,
                            onComplete = ::onboardingComplete,
                            onChangeUseTTS = ::onChangeUseTTS,
                            onRequestReadNotificationsPermission = ::onRequestReadNotificationsPermission,
                            onRequestShowNotificationPermission = ::onRequestShowNotificationPermission,
                            onRequestBtPermission = ::onRequestBtPermission
                        )
                }
            }
        }

        lifecycleScope.launch {
            BTRMDataStore.getValueFlow(VOICE_NOTIFICATION_APPS, this@ComposeActivity)
                .map { it?.getList() ?: emptyList() }
                .map { it.filter { packageName -> isAppInstalled(packageName) } }
                .map { installedApps ->
                    installedApps.map { app ->
                        val appInfo =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                packageManager.getApplicationInfo(
                                    app,
                                    PackageManager.ApplicationInfoFlags.of(0)
                                )
                            } else {
                                packageManager.getApplicationInfo(app, 0)
                            }
                        val name = packageManager.getApplicationLabel(appInfo).toString()
                        val icon = packageManager.getApplicationIcon(app)

                        InstalledApp(
                            packageName = app,
                            name = name,
                            icon = icon
                        )
                    }
                }
                .collect { preparedApps ->
                    viewModel.onSetInstalledApps(preparedApps)
                }
            }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onSetReadNotificationsPermission(isNotificationServiceRunning())
        viewModel.onSetPostNotificationPermission(checkPostNotificationPermission())

        val btPermission = checkBtPermission()
        viewModel.onSetBTStatusPermission(btPermission)
        if (btPermission)
            getInitialBluetoothStatus(viewModel::onChangeBtStatus)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(notificationBroadcastReceiver)
        unregisterReceiver(btBroadcastReceiver)
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceivers() {
        val notificationsIntentFilter = IntentFilter().apply {
            addAction("com.katdmy.android.bluetoothreadermusic.onNotificationPosted")
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
    }

    private fun checkPostNotificationPermission() : Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else
            false

    private fun checkBtPermission() : Boolean =
        checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun isNotificationServiceRunning(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        return enabled.contains(packageName)
    }

    private fun initTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInitialized()
            }
        }

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        focusRequest =
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE).run {
                setAudioAttributes(AudioAttributes.Builder().run {
                    setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    build()
                })
                build()
            }
    }

    private fun ttsInitialized() {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                audioManager.requestAudioFocus(focusRequest)
                viewModel.onSetReadingTestText(true)
            }

            override fun onDone(utteranceId: String) {
                audioManager.abandonAudioFocusRequest(focusRequest)
                viewModel.onSetReadingTestText(false)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                audioManager.abandonAudioFocusRequest(focusRequest)
                viewModel.onSetReadingTestText(false)
            }
        })
    }

    private fun onClickReadTestText(text: String) {
        if (getCurrentVolumePercent() < 10)
            Toast.makeText(this, getString(R.string.low_volume), Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val randomVoice = BTRMDataStore.getValue(RANDOM_VOICE, this@ComposeActivity)
            if (randomVoice == true) {
                if (tts.voices == null) {
                    tts.speak(text, TextToSpeech.QUEUE_ADD, null, text)
                } else {
                    tts.voice = tts.voices.filter { it.locale.language == Locale.getDefault().language }.random()
                    tts.speak(text, TextToSpeech.QUEUE_ADD, null, text)
                }
            } else {
                tts.speak(text, TextToSpeech.QUEUE_ADD, null, text)
            }
        }
    }

    private fun onClickStopReading() {
        tts.stop()
        viewModel.onSetReadingTestText(false)
    }

    private fun restartNotificationListener() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun getCurrentVolumePercent(): Int {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return (currentVolume * 100) / maxVolume
    }

    private fun onClickDeleteApp(deletingApp: String) {
        lifecycleScope.launch {
            val addedAppsList = BTRMDataStore.getValue(VOICE_NOTIFICATION_APPS, this@ComposeActivity)?.getList() ?: listOf()
            val newAddedAppsList = addedAppsList.filter { it != deletingApp }.getString()
            BTRMDataStore.saveValue(newAddedAppsList, VOICE_NOTIFICATION_APPS, this@ComposeActivity)
        }
    }

    private fun onAddApps(newApps: List<String>) {
        lifecycleScope.launch {
            val addedAppsList = BTRMDataStore.getValue(VOICE_NOTIFICATION_APPS, this@ComposeActivity)?.getList() ?: listOf()
            val newAddedAppsList = addedAppsList.plus(newApps).getString()
            BTRMDataStore.saveValue(newAddedAppsList, VOICE_NOTIFICATION_APPS, this@ComposeActivity)
        }
    }

    private fun onChangeUseTTS(useTTS: Boolean) {
        lifecycleScope.launch {
            BTRMDataStore.saveValue(useTTS, USE_TTS_SF, this@ComposeActivity)
        }
    }

    private fun onSetTtsMode(newTtsMode: Int) {
        lifecycleScope.launch {
            BTRMDataStore.saveValue(newTtsMode, TTS_MODE, this@ComposeActivity)
        }
    }

    private fun getInstalledLaunchableApps(): List<InstalledApp> {
        val pm = applicationContext.packageManager

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            pm.queryIntentActivities(intent, 0)
        }

        return resolveInfos
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .map { appInfo ->
                InstalledApp(
                    packageName = appInfo.packageName,
                    name = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo)
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    private fun onSetRandomVoice(newRandomVoice: Boolean) {
        lifecycleScope.launch {
            BTRMDataStore.saveValue(newRandomVoice, RANDOM_VOICE, this@ComposeActivity)
        }
    }

    private fun onClickAbandonAudiofocus() {
        val intent = Intent("com.katdmy.android.bluetoothreadermusic.abandonAudiofocus")
        sendBroadcast(intent)
    }

    private fun getInitialBluetoothStatus(setToModel: (String) -> Unit) {
        BluetoothConnectionChecker(this, setToModel)
    }

    private fun onboardingComplete() {
        lifecycleScope.launch {
            BTRMDataStore.saveValue(true, ONBOARDING_COMPLETE, this@ComposeActivity)
        }
    }

    private fun onRequestReadNotificationsPermission() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun onRequestShowNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }
    }

    private fun onRequestBtPermission() {
        requestPermissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
    }
}



