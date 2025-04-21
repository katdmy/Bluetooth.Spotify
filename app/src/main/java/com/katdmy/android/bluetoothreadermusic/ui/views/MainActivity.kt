package com.katdmy.android.bluetoothreadermusic.ui.views

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
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
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.util.Constants.MUSIC_PACKAGE_NAME
import com.katdmy.android.bluetoothreadermusic.util.Constants.USE_TTS_SF
import com.katdmy.android.bluetoothreadermusic.data.*
import com.katdmy.android.bluetoothreadermusic.data.models.MessengerApp
import com.katdmy.android.bluetoothreadermusic.data.models.MusicApp
import com.katdmy.android.bluetoothreadermusic.receivers.BtBroadcastReceiver
import com.katdmy.android.bluetoothreadermusic.receivers.NotificationBroadcastReceiver
import com.katdmy.android.bluetoothreadermusic.services.ListenerStatusService
import com.katdmy.android.bluetoothreadermusic.services.NotificationListener
import com.katdmy.android.bluetoothreadermusic.ui.theme.BtReaderMusicTheme
import com.katdmy.android.bluetoothreadermusic.ui.vm.MainViewModel
import com.katdmy.android.bluetoothreadermusic.util.BTRMDataStore
import com.katdmy.android.bluetoothreadermusic.util.BluetoothConnectionChecker
import com.katdmy.android.bluetoothreadermusic.util.StringListHelper.getList
import com.katdmy.android.bluetoothreadermusic.util.StringListHelper.getString
import com.katdmy.android.bluetoothreadermusic.util.Constants.ENABLED_MESSENGERS
import com.katdmy.android.bluetoothreadermusic.util.Constants.IGNORE_LACK_OF_PERMISSION
import com.katdmy.android.bluetoothreadermusic.util.Constants.ONBOARDING_COMPLETE
import com.katdmy.android.bluetoothreadermusic.util.Constants.RANDOM_VOICE
import com.katdmy.android.bluetoothreadermusic.util.Constants.SERVICE_STARTED
import com.katdmy.android.bluetoothreadermusic.util.Constants.TTS_MODE
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.core.net.toUri

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
        initMusicApps(viewModel::onSetInstalledMusicApps)
        initMessengerApps(viewModel::onSetInstalledMessengerApps)

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
        val intent = Intent(this@ComposeActivity, ListenerStatusService::class.java)
        startForegroundService(intent)

        setContent {
            val isOnboardingComplete by BTRMDataStore.getValueFlow(ONBOARDING_COMPLETE, this).collectAsState(initial = false)

            BtReaderMusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isOnboardingComplete == true)
                        MainLayout(
                            viewModel,
                            onClickReadTestText = ::onClickReadTestText,
                            onClickStopReading = ::onClickStopReading,
                            onClickStopService = ::onClickStopService,
                            onClickStartService = ::onClickStartService,
                            onClickServiceStatus = ::onClickServiceStatus,
                            onSelectMusicApp = ::onSelectMusicApp,
                            onChangeUseTTS = ::onChangeUseTTS,
                            onSetTtsMode = ::onSetTtsMode,
                            onCheckedChangeMessengerApp = ::onCheckedChangeMessengerApp,
                            onSetRandomVoice = ::onSetRandomVoice,
                            onClickRequestReadNotificationsPermission = ::onRequestReadNotificationsPermission,
                            onClickRequestPostNotificationPermission = ::onRequestShowNotificationPermission,
                            onClickRequestBtPermission = ::onRequestBtPermission,
                            onClickAbandonAudiofocus = ::onClickAbandonAudiofocus,
                            onClickOpenMusic = ::onClickOpenMusic,
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
    }

    override fun onResume() {
        super.onResume()
        viewModel.onSetReadNotificationsPermission(isNotificationServiceRunning())
        viewModel.onSetPostNotificationPermission(checkPostNotificationPermission())

        val btPermission = checkBtPermission()
        viewModel.onSetBTStatusPermission(btPermission)
        if (btPermission == true)
            getInitialBluetoothStatus(viewModel::onChangeBtStatus)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(notificationBroadcastReceiver)
        unregisterReceiver(btBroadcastReceiver)
    }

    private fun initMusicApps(setToModel: (ArrayList<MusicApp>) -> Unit) {
        val installedMusicApps: ArrayList<MusicApp> = arrayListOf()
        installedMusicApps.clear()
        val musicAppList = listOf(
            "ru.yandex.music",
            "com.spotify.music",
            "com.google.android.apps.youtube.music",
            "deezer.android.app",
            "com.apple.android.music"
        )
        for (app in musicAppList) {
            if (isAppInstalled(app)) {
                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getApplicationInfo(
                        app,
                        PackageManager.ApplicationInfoFlags.of(0)
                    )
                } else {
                    packageManager.getApplicationInfo(app, 0)
                }
                val name = packageManager.getApplicationLabel(appInfo).toString()
                val launchIntent = packageManager.getLaunchIntentForPackage(app)
                val icon = packageManager.getApplicationIcon(app)
                installedMusicApps.add(MusicApp(app, launchIntent, name, icon))
            }
        }
        setToModel(installedMusicApps)

        lifecycleScope.launch {
            val previouslySelectedMusicAppPackageName =
                BTRMDataStore.getValue(MUSIC_PACKAGE_NAME, this@ComposeActivity)
            viewModel.onSelectMusicAppByPackageName(previouslySelectedMusicAppPackageName)
        }
    }

    private fun initMessengerApps(setToModel: (ArrayList<MessengerApp>) -> Unit) {
        val installedMessengerApps: ArrayList<MessengerApp> = arrayListOf()
        installedMessengerApps.clear()
        val messengerAppList = listOf(
            "com.whatsapp",
            "org.telegram.messenger",
            "com.tencent.mm",
            "com.instagram.android",
            "com.google.android.apps.messaging"
        )
        for (app in messengerAppList) {
            if (isAppInstalled(app)) {
                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getApplicationInfo(
                        app,
                        PackageManager.ApplicationInfoFlags.of(0)
                    )
                } else {
                    packageManager.getApplicationInfo(app, 0)
                }
                val name = packageManager.getApplicationLabel(appInfo).toString()
                val icon = packageManager.getApplicationIcon(app)
                installedMessengerApps.add(
                    MessengerApp(
                        packageName = app,
                        name = name,
                        icon = icon
                    )
                )
            }
        }
        setToModel(installedMessengerApps)
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
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationListenerAccessGranted(
            ComponentName(this@ComposeActivity, NotificationListener::class.java)
        )
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

    private fun getCurrentVolumePercent(): Int {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return (currentVolume * 100) / maxVolume
    }

    private fun onClickStopService() {
        lifecycleScope.launch {
            BTRMDataStore.saveValue(false, SERVICE_STARTED, this@ComposeActivity)
        }

        val intent = Intent("com.katdmy.android.bluetoothreadermusic.stopStatusService")
        sendBroadcast(intent)
    }

    private fun onClickStartService() {
        packageManager.setComponentEnabledSetting(
            ComponentName(
                this,
                NotificationListener::class.java
            ), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
        )
        Thread.sleep(500)
        packageManager.setComponentEnabledSetting(
            ComponentName(
                this,
                NotificationListener::class.java
            ), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
        )

        if (!isNotificationServiceRunning()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        lifecycleScope.launch {
            BTRMDataStore.saveValue(true, SERVICE_STARTED, this@ComposeActivity)
            BTRMDataStore.saveValue(false, IGNORE_LACK_OF_PERMISSION, this@ComposeActivity)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestForegroundServicePermissionIfNeeded(this)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.FOREGROUND_SERVICE), 100)
        }
        val intent = Intent(this, ListenerStatusService::class.java)
        startForegroundService(intent)
    }

    private fun onClickServiceStatus() {
        val result = if (isNotificationServiceRunning()) getString(R.string.service_started) else getString(R.string.service_stopped)
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
    }

    fun requestForegroundServicePermissionIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+
            val intent = Intent("android.settings.ACTION_REQUEST_FOREGROUND_SERVICE_SPECIAL_USE_PERMISSION").apply {
                data = "package:${context.packageName}".toUri()
            }
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.e("MainActivity", "Foreground Service Special Use permission screen not found!", e)
            }
        }
    }

    private fun onSelectMusicApp(selectedMusicApp: MusicApp) {
        viewModel.onSelectMusicApp(selectedMusicApp)
        lifecycleScope.launch {
            BTRMDataStore.saveValue(selectedMusicApp.packageName, MUSIC_PACKAGE_NAME, this@ComposeActivity)
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

    private fun onCheckedChangeMessengerApp(messengerAppPackageName: String, isChecked: Boolean) {
        lifecycleScope.launch {
            val enabledMessengersList = BTRMDataStore.getValue(ENABLED_MESSENGERS, this@ComposeActivity)?.getList() ?: listOf()
            val newEnabledMessagesString = if (isChecked)
                enabledMessengersList.plus(messengerAppPackageName).getString()
            else
                enabledMessengersList.filter { it != messengerAppPackageName }.getString()
            BTRMDataStore.saveValue(newEnabledMessagesString, ENABLED_MESSENGERS, this@ComposeActivity)
        }
    }

    private fun onClickOpenMusic(launchMusicAppIntent: Intent?) {
        launchMusicAppIntent?.let { startActivity(it) }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainLayout(
    viewModel: MainViewModel,
    onClickReadTestText: (String) -> Unit,
    onClickStopReading: () -> Unit,
    onClickStopService: () -> Unit,
    onClickStartService: () -> Unit,
    onClickServiceStatus: () -> Unit,
    onSelectMusicApp: (MusicApp) -> Unit,
    onChangeUseTTS: (Boolean) -> Unit,
    onSetTtsMode: (Int) -> Unit,
    onCheckedChangeMessengerApp: (String, Boolean) -> Unit,
    onSetRandomVoice: (Boolean) -> Unit,
    onClickRequestReadNotificationsPermission: () -> Unit,
    onClickRequestPostNotificationPermission: () -> Unit,
    onClickRequestBtPermission: () -> Unit,
    onClickAbandonAudiofocus: () -> Unit,
    onClickOpenMusic: (launchMusicAppIntent: Intent?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val state = viewModel.uiState.collectAsState()
    val isReadingTestText = viewModel.isReadingTestText.collectAsState()
    val permissions = viewModel.permissionState.collectAsState()
    val context = LocalContext.current
    var navigation: Navigation by remember { mutableStateOf(Navigation.Main) }
    var testTextToSpeech by remember { mutableStateOf("") }
    val useTTS by BTRMDataStore.getValueFlow(USE_TTS_SF, context).collectAsState(initial = false)
    val enabledMessengerString by BTRMDataStore.getValueFlow(ENABLED_MESSENGERS, context).collectAsState(initial = "")
    val ttsModeSelection by BTRMDataStore.getValueFlow(TTS_MODE, context).collectAsState(initial = 0)
    val randomVoice by BTRMDataStore.getValueFlow(RANDOM_VOICE, context).collectAsState(initial = false)

    Scaffold(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars)
            .consumeWindowInsets(WindowInsets.statusBars),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = navigation,
                        transitionSpec = { slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut() using
                                SizeTransform(clip = false)
                                         },
                        label = "Screen Header"
                    ) {
                        when (it) {
                            Navigation.SettingsScreen -> {
                                Text(stringResource(R.string.settings_header))
                            }
                            Navigation.Main -> {
                                Text(stringResource(R.string.main_screen_header))
                            }
                            Navigation.PrivacyPolicyScreeen -> {
                                Text(stringResource(R.string.privacy_policy_header))
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (navigation != Navigation.Main)
                        IconButton(onClick = {
                            navigation = when (navigation) {
                                Navigation.SettingsScreen -> Navigation.Main
                                Navigation.PrivacyPolicyScreeen -> Navigation.SettingsScreen
                                else -> Navigation.Main
                            }
                        } ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                },
                actions = {
                    if (navigation == Navigation.Main)
                        IconButton(onClick = { navigation = Navigation.SettingsScreen } ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Open/close settings"
                            )
                        }
                },
                modifier = Modifier
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = navigation,
            transitionSpec = { slideInHorizontally { -it } + fadeIn() togetherWith
                    slideOutHorizontally { it } + fadeOut() using
                    SizeTransform(clip = false)
            },
            label = "Screen Contents",
            modifier = Modifier.padding(paddingValues)
        ) {
            when (it) {
                Navigation.SettingsScreen -> {
                    SettingsScreen(
                        scope = scope,
                        snackbarHostState = snackbarHostState,
                        ttsModeSelection = ttsModeSelection,
                        installedMessengers = state.value.installedMessengerApps,
                        enabledMessengerString = enabledMessengerString,
                        installedMusicApps = state.value.installedMusicApps,
                        selectedMusicApp = state.value.selectedMusicApp,
                        randomVoice = randomVoice,
                        postNotificationPermissionGranted = permissions.value.postNotification,
                        readNotificationsPermissionGranted = permissions.value.readNotifications,
                        btStatusPermissionGranted = permissions.value.btStatus,
                        btStatus = state.value.btStatus,
                        onSetTtsMode = onSetTtsMode,
                        onCheckedChangeMessengerApp = onCheckedChangeMessengerApp,
                        onSelectMusicApp = onSelectMusicApp,
                        onSetRandomVoice = onSetRandomVoice,
                        onClickStopService = onClickStopService,
                        onClickStartService = onClickStartService,
                        onClickServiceStatus = onClickServiceStatus,
                        onClickRequestReadNotificationsPermission = onClickRequestReadNotificationsPermission,
                        onClickRequestPostNotificationPermission = onClickRequestPostNotificationPermission,
                        onClickRequestBtPermission = onClickRequestBtPermission,
                        onClickAbandonAudiofocus = onClickAbandonAudiofocus,
                        onClickPrivacyPolicy = { navigation = Navigation.PrivacyPolicyScreeen }
                    )
                }
                Navigation.Main -> {
                    MainScreen(
                        testTextToSpeech = testTextToSpeech,
                        onTestTextToSpeechChange = { newText -> testTextToSpeech = newText },
                        logMessages = state.value.logMessages,
                        selectedMusicApp = state.value.selectedMusicApp,
                        useTTS = useTTS == true,
                        isReadingTestText = isReadingTestText.value,
                        onClickReadTestText = onClickReadTestText,
                        onClickStopReading = onClickStopReading,
                        onClearLog = viewModel::onClearLogMessages,
                        onChangeUseTTS = { newUseTTS ->
                            if (permissions.value.readNotifications)
                                onChangeUseTTS(newUseTTS)
                            else
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.no_read_notifications_permission),
                                        actionLabel = context.getString(R.string.enable_permission),
                                        duration = SnackbarDuration.Long
                                    )
                                    when (result) {
                                        SnackbarResult.ActionPerformed -> onClickRequestReadNotificationsPermission()
                                        SnackbarResult.Dismissed -> {}
                                    }
                                }
                        },
                        onClickOpenMusic = onClickOpenMusic
                    )
                }
                Navigation.PrivacyPolicyScreeen -> {
                    PrivacyPolicyScreeen(
                        context.getString(R.string.privacy_policy_url)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainLayoutPreview() {
    BtReaderMusicTheme {
        MainScreen(
            testTextToSpeech = "",
            onTestTextToSpeechChange = {},
            logMessages = "",
            useTTS = false,
            selectedMusicApp = MusicApp("com.spotify.music", null, "Spotify", null),
            isReadingTestText = false,
            onClickReadTestText = {},
            onClickStopReading = {},
            onClearLog = {},
            onChangeUseTTS = {},
            onClickOpenMusic = {}
        )
    }
}

