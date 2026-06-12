package com.katdmy.android.bluetoothreadermusic.ui.views

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
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
import com.katdmy.android.bluetoothreadermusic.data.enums.AudioFocusMode
import com.katdmy.android.bluetoothreadermusic.data.enums.NotificationPart
import com.katdmy.android.bluetoothreadermusic.data.models.AppVoiceSettings
import com.katdmy.android.bluetoothreadermusic.util.Constants.USE_TTS_SF
import com.katdmy.android.bluetoothreadermusic.data.models.InstalledApp
import com.katdmy.android.bluetoothreadermusic.services.StatusService
import com.katdmy.android.bluetoothreadermusic.ui.theme.BtReaderMusicTheme
import com.katdmy.android.bluetoothreadermusic.ui.vm.MainViewModel
import com.katdmy.android.bluetoothreadermusic.util.BTConnectionState
import com.katdmy.android.bluetoothreadermusic.util.BTRMDataStore
import com.katdmy.android.bluetoothreadermusic.util.BluetoothConnectionChecker
import com.katdmy.android.bluetoothreadermusic.util.Constants.APP_VOICE_SETTINGS
import com.katdmy.android.bluetoothreadermusic.util.Constants.GLOBAL_AUDIOFOCUS_MODE
import com.katdmy.android.bluetoothreadermusic.util.Constants.GLOBAL_NOTIFICATION_PARTS
import com.katdmy.android.bluetoothreadermusic.util.StringListHelper.getList
import com.katdmy.android.bluetoothreadermusic.util.Constants.ONBOARDING_COMPLETE
import com.katdmy.android.bluetoothreadermusic.util.Constants.RANDOM_VOICE
import com.katdmy.android.bluetoothreadermusic.util.Constants.SHOW_LOG
import com.katdmy.android.bluetoothreadermusic.util.Constants.TTS_MODE
import com.katdmy.android.bluetoothreadermusic.util.Constants.TTS_VOLUME
import kotlinx.coroutines.launch
import java.util.Locale
import com.katdmy.android.bluetoothreadermusic.util.Constants.VOICE_NOTIFICATION_APPS
import com.katdmy.android.bluetoothreadermusic.util.DebugLog
import com.katdmy.android.bluetoothreadermusic.util.PackageHelper.getAppIcon
import com.katdmy.android.bluetoothreadermusic.util.PackageHelper.getAppName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ComposeActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel>()
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var tts: TextToSpeech
    private lateinit var audioManager: AudioManager
    private lateinit var focusRequest: AudioFocusRequest
    private var currentVoiceName: String = ""
    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        initTTS()

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionAndGrant ->
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
                    getInitialBluetoothStatus(BTConnectionState::set)
            }
        }

        val intent = Intent(this@ComposeActivity, StatusService::class.java)
        startForegroundService(intent)

        setContent {
            val isOnboardingComplete by BTRMDataStore.getValueFlow(ONBOARDING_COMPLETE, this)
                .collectAsState(initial = false)

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
                            onClickSaveAppSettings = ::onClickSaveAppSettings,
                            onAddApps = ::onAddApps,
                            restartNotificationListener = ::restartNotificationListener,
                            onChangeUseTTS = ::onChangeUseTTS,
                            onSetTtsMode = ::onSetTtsMode,
                            onSetRandomVoice = ::onSetRandomVoice,
                            onSetTtsVolume = ::onSetTtsVolume,
                            onClickRequestReadNotificationsPermission = ::onRequestReadNotificationsPermission,
                            onClickRequestPostNotificationPermission = ::onRequestShowNotificationPermission,
                            onClickRequestBtPermission = ::onRequestBtPermission,
                            onChangeShowLog = ::onChangeShowLog,
                            onClickForceRestartTTS = ::onClickForceRestartTTS,
                            onClickOpenTTSSettings = ::onClickOpenTTSSettings,
                            onChangeGlobalAudioFocusMode = ::onChangeGlobalAudioFocusMode,
                            onChangeGlobalNotificationParts = ::onChangeGlobalNotificationParts
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
            launch {
                // TODO("Migration to new installed app list with audio settings")
                val oldAppString = BTRMDataStore.getValue(VOICE_NOTIFICATION_APPS, this@ComposeActivity)
                val newAppString = BTRMDataStore.getValue(APP_VOICE_SETTINGS, this@ComposeActivity)
                if (!oldAppString.isNullOrBlank() && newAppString.isNullOrBlank()) {
                    val oldAppList = oldAppString.getList()
                    val newApps: MutableList<AppVoiceSettings> = mutableListOf()
                    for (oldApp in oldAppList) {
                        newApps.add(AppVoiceSettings(oldApp))
                    }
                    val newAppsRaw = json.encodeToString(newApps)
                    BTRMDataStore.saveValue(newAppsRaw, APP_VOICE_SETTINGS, this@ComposeActivity)
                    BTRMDataStore.removeValue(VOICE_NOTIFICATION_APPS, this@ComposeActivity)
                }
                // TODO("Remove this block in new version")
            }
            launch {
                BTRMDataStore.getValueFlow(APP_VOICE_SETTINGS, this@ComposeActivity)
                    .map { raw ->
                        if (raw != null)
                            try {
                                json.decodeFromString<List<AppVoiceSettings>>(raw)
                            } catch (_: Exception) {
                                emptyList()
                            }
                        else
                            emptyList()
                    }
                    .distinctUntilChanged()
                    .map { it.filter { appVoiceSettings -> isAppInstalled(appVoiceSettings.packageName) } }
                    .map { allSettings ->

                        withContext(Dispatchers.IO) {
                            val installedApps: MutableList<InstalledApp> = mutableListOf()
                            for (appVoiceSettings in allSettings) {
                                val name = getAppName(this@ComposeActivity, appVoiceSettings.packageName)
                                val icon = getAppIcon(this@ComposeActivity, appVoiceSettings.packageName)

                                installedApps.add(
                                    InstalledApp(
                                        packageName = appVoiceSettings.packageName,
                                        name = name,
                                        icon = icon
                                    )
                                )
                            }
                            Pair(allSettings, installedApps)
                        }
                    }
                    .collect { (appVoiceSettings, installedApps) ->
                        viewModel.onSetAddedApps(installedApps)
                        viewModel.onSetAllAppSettings(appVoiceSettings)
                    }
            }
            launch {
                BTRMDataStore.getValueFlow(
                        GLOBAL_AUDIOFOCUS_MODE,
                        this@ComposeActivity
                    )
                    .map { raw ->
                        try {
                            raw?.let {
                                json.decodeFromString<AudioFocusMode>(it)
                            }
                        } catch (_: Exception) {
                            null
                        } ?: AudioFocusMode.DUCK
                    }
                    .collect(viewModel::onSetGlobalAudioFocusMode)
            }
            launch {
                BTRMDataStore.getValueFlow(
                        GLOBAL_NOTIFICATION_PARTS,
                        this@ComposeActivity
                    )
                    .map { raw ->
                        try {
                            raw?.let {
                                json.decodeFromString<Set<NotificationPart>>(it)
                            }
                        } catch (_: Exception) {
                            null
                        } ?: setOf(
                            NotificationPart.TITLE,
                            NotificationPart.TEXT
                        )
                    }
                    .collect(viewModel::onSetGlobalNotificationParts)
            }
            launch {
                BTRMDataStore.getValueFlow(
                        APP_VOICE_SETTINGS,
                        this@ComposeActivity
                    )
                    .map { raw ->
                        try {
                            raw?.let {
                                json.decodeFromString<List<AppVoiceSettings>>(it)
                            }
                        } catch (_: Exception) {
                            null
                        } ?: emptyList()
                    }
                    .collect(viewModel::onSetAllAppSettings)
            }
        }
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onSetReadNotificationsPermission(isNotificationServiceRunning())
        viewModel.onSetPostNotificationPermission(checkPostNotificationPermission())

        val btPermission = checkBtPermission()
        viewModel.onSetBTStatusPermission(btPermission)
        if (btPermission)
            getInitialBluetoothStatus(BTConnectionState::set)
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
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).run {
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

        val voicesCount = tts.voices?.count {
            it.locale?.language == Locale.getDefault().language &&
                    !it.isNetworkConnectionRequired
        } ?: 0
        viewModel.onSetVoicesCount(voicesCount)
    }

    private fun onClickReadTestText(text: String) {
        if (getCurrentVolumePercent() < 10)
            Toast.makeText(this, getString(R.string.low_volume), Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val useRandomVoice = BTRMDataStore.getValue(RANDOM_VOICE, this@ComposeActivity)
            if (useRandomVoice == true) {
                val targetLang = Locale.getDefault().language
                val randomVoices = tts.voices.filter { voice ->
                    voice.locale?.language == targetLang &&
                            !voice.isNetworkConnectionRequired &&
                            voice.quality >= Voice.QUALITY_NORMAL
                }

                val nextVoice = randomVoices
                    .filter { it.name != currentVoiceName }
                    .randomOrNull()

                if (nextVoice == null) {
                    tts.language = Locale.getDefault()
                    DebugLog.add("Error changing voice, using default voice instead. Try check available voices list on Settings screen")
                } else {
                    try {
                        tts.voice = nextVoice
                        currentVoiceName = nextVoice.name
                        DebugLog.add("Voice selected: ${nextVoice.name}")
                    } catch (_: Exception) {
                        tts.language = Locale.getDefault()
                        DebugLog.add("Error setting voice: ${nextVoice.name}")
                    }
                }
            }

            val volume = BTRMDataStore.getValue(TTS_VOLUME, this@ComposeActivity) ?: 1f
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume*volume)
            }
            val utteranceId = System.nanoTime().toString()
            tts.speak(text, TextToSpeech.QUEUE_ADD, params, utteranceId)
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
            val addedSettingsRaw = BTRMDataStore.getValue(APP_VOICE_SETTINGS, this@ComposeActivity) ?: ""
            val allSettingsList =
                try {
                    json.decodeFromString<List<AppVoiceSettings>>(addedSettingsRaw)
                } catch (_: Exception) {
                    emptyList()
                }
            val listWithNewSettings = allSettingsList.filter { it.packageName != deletingApp }
            val updatedSettingsRaw = json.encodeToString(listWithNewSettings)
            BTRMDataStore.saveValue(updatedSettingsRaw, APP_VOICE_SETTINGS, this@ComposeActivity)
        }
    }

    private fun onClickSaveAppSettings(appVoiceSettings: AppVoiceSettings) {
        lifecycleScope.launch {
            val allSettingsRaw = BTRMDataStore.getValue(APP_VOICE_SETTINGS, this@ComposeActivity) ?: ""
            val allSettingsList =
                try {
                    json.decodeFromString<List<AppVoiceSettings>>(allSettingsRaw)
                } catch (_: Exception) {
                    emptyList()
                }
            val listWithNewSettings =
                allSettingsList
                    .filter { it.packageName != appVoiceSettings.packageName }
                    .toMutableList()
                    .apply {
                        add(appVoiceSettings)
                    }
            val updatedSettingsRaw = json.encodeToString(listWithNewSettings)
            BTRMDataStore.saveValue(updatedSettingsRaw, APP_VOICE_SETTINGS, this@ComposeActivity)
        }
    }

    private fun onAddApps(newApps: List<String>) {
        lifecycleScope.launch {
            val raw = BTRMDataStore.getValue(APP_VOICE_SETTINGS, this@ComposeActivity) ?: ""
            val list =
                try {
                    json.decodeFromString<List<AppVoiceSettings>>(raw)
                } catch (_: Exception) {
                    emptyList()
                }
            val newList = list + newApps.map {
                packageName -> AppVoiceSettings(packageName)
            }
            val newRaw = json.encodeToString(newList)
            BTRMDataStore.saveValue(newRaw, APP_VOICE_SETTINGS, this@ComposeActivity)
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

    private fun getInstalledLaunchableApps() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val pm = applicationContext.packageManager

                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }

                val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
                } else {
                    pm.queryIntentActivities(intent, 0)
                }

                viewModel.onSetInstalledApps(
                    resolveInfos
                        .map { it.activityInfo.applicationInfo }
                        .distinctBy { it.packageName }
                        .map { appInfo ->
                            InstalledApp(
                                packageName = appInfo.packageName,
                                name = pm.getApplicationLabel(appInfo).toString(),
                                icon = pm.getApplicationIcon(appInfo)
                            )
                        }
                        .sortedBy { it.name?.lowercase() ?: it.packageName.lowercase() }
                )
            }
        }
    }

    private fun onSetRandomVoice(newRandomVoice: Boolean) {
        lifecycleScope.launch {
            BTRMDataStore.saveValue(newRandomVoice, RANDOM_VOICE, this@ComposeActivity)
        }
    }

    private fun onSetTtsVolume(newVolume: Float) {
        lifecycleScope.launch {
            BTRMDataStore.saveValue(newVolume, TTS_VOLUME, this@ComposeActivity)
        }
    }

    private fun onChangeShowLog(newShowLog: Boolean) {
        lifecycleScope.launch {
            BTRMDataStore.saveValue(newShowLog, SHOW_LOG, this@ComposeActivity)
        }
    }

    private fun onClickForceRestartTTS() {
        val intent = Intent("com.katdmy.android.bluetoothreadermusic.forceRestartTTS")
        sendBroadcast(intent)

        try {
            tts.stop()
            tts.setOnUtteranceProgressListener(null)
            tts.shutdown()
        } catch (_: Exception) {
        }

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) ttsInitialized()
        }
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

    private fun onClickOpenTTSSettings() {
        startActivity(Intent("com.android.settings.TTS_SETTINGS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    private fun onChangeGlobalAudioFocusMode(newAudioFocusMode: AudioFocusMode) {
        lifecycleScope.launch {
            val raw = json.encodeToString(newAudioFocusMode)
            BTRMDataStore.saveValue(raw, GLOBAL_AUDIOFOCUS_MODE, this@ComposeActivity)
        }
    }

    private fun onChangeGlobalNotificationParts(newNotificationParts: Set<NotificationPart>) {
        lifecycleScope.launch {
            val raw = json.encodeToString(newNotificationParts)
            BTRMDataStore.saveValue(raw, GLOBAL_NOTIFICATION_PARTS, this@ComposeActivity)
        }
    }
}