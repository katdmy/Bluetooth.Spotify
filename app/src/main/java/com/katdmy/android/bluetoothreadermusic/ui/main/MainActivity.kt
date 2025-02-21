package com.katdmy.android.bluetoothreadermusic.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
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
//import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.util.Constants.MUSIC_PACKAGE_NAME
import com.katdmy.android.bluetoothreadermusic.util.Constants.USE_TTS_SF
import com.katdmy.android.bluetoothreadermusic.data.*
import com.katdmy.android.bluetoothreadermusic.receivers.BtBroadcastReceiver
import com.katdmy.android.bluetoothreadermusic.receivers.NotificationBroadcastReceiver
import com.katdmy.android.bluetoothreadermusic.services.NotificationListener
import com.katdmy.android.bluetoothreadermusic.ui.onboarding.OnboardingScreen
import com.katdmy.android.bluetoothreadermusic.ui.theme.BtReaderMusicTheme
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

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
        viewModel.onSetReadingTestText(false)

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { permissionAndGrant ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (permissionAndGrant.keys.contains(Manifest.permission.POST_NOTIFICATIONS)) {
                        viewModel.onSetPostNotificationPermission(permissionAndGrant[Manifest.permission.POST_NOTIFICATIONS] == true)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (permissionAndGrant.keys.contains(Manifest.permission.BLUETOOTH_CONNECT)) {
                        viewModel.onSetBTStatusPermission(permissionAndGrant[Manifest.permission.BLUETOOTH_CONNECT] == true)
                        if (permissionAndGrant[Manifest.permission.BLUETOOTH_CONNECT] == true)
                            getInitialBluetoothStatus(viewModel::onChangeBtStatus)
                    }
                }
            }

        registerReceivers()
        lifecycleScope.launch {
            val servicePreviouslyStarted = BTRMDataStore.getValue(SERVICE_STARTED, this@ComposeActivity)
            if (servicePreviouslyStarted == true) {
                packageManager.setComponentEnabledSetting(
                    ComponentName(
                        this@ComposeActivity,
                        NotificationListener::class.java
                    ), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
                )
                packageManager.setComponentEnabledSetting(
                    ComponentName(
                        this@ComposeActivity,
                        NotificationListener::class.java
                    ), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
                )
            }
        }

        setContent {
            val isOnboardingComplete by BTRMDataStore.getValueFlow(ONBOARDING_COMPLETE, this).collectAsState(initial = false)

            BtReaderMusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isOnboardingComplete == true)
                        MainScreen(
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
                            onClickOpenMusic = ::onClickOpenMusic
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (btPermission == true)
                getInitialBluetoothStatus(viewModel::onChangeBtStatus)
        }
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
        //Log.e("InstalledMusicApps", installedMusicApps.toString())

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
        //Log.e("InstalledMessengerApps", installedMessengerApps.toString())
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else
            false

    private fun isNotificationServiceRunning(): Boolean {
        val enabledNotificationListeners =
            Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabledNotificationListeners != null && enabledNotificationListeners.contains(
            packageName
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
            viewModel.onAddLogMessage(text)
        }
    }

    private fun onClickStopReading() {
        tts.stop()
        viewModel.onSetReadingTestText(false)
    }

    private fun onClickStopService() {
        packageManager.setComponentEnabledSetting(
            ComponentName(
                this,
                NotificationListener::class.java
            ), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
        )
        lifecycleScope.launch {
            BTRMDataStore.saveValue(false, SERVICE_STARTED, this@ComposeActivity)
        }
    }
    private fun onClickStartService() {
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
        lifecycleScope.launch {
            BTRMDataStore.saveValue(true, SERVICE_STARTED, this@ComposeActivity)
            BTRMDataStore.saveValue(false, IGNORE_LACK_OF_PERMISSION, this@ComposeActivity)
        }
    }

    private fun onClickServiceStatus() {
        val result = if (isNotificationServiceRunning()) getString(R.string.service_started) else getString(R.string.service_stopped)
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
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
            //Log.e("ENABLED_MESSENGERS", newEnabledMessagesString)
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
        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }

    private fun onRequestShowNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }
    }

    private fun onRequestBtPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
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
    onClickOpenMusic: (launchMusicAppIntent: Intent?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val state = viewModel.uiState.collectAsState()
    val isReadingTestText = viewModel.isReadingTestText.collectAsState()
    val permissions = viewModel.permissionState.collectAsState()
    val context = LocalContext.current
    var settingsShown by remember { mutableStateOf(false) }
    var testTextToSpeech by remember { mutableStateOf("") }
    val useTTS by BTRMDataStore.getValueFlow(USE_TTS_SF, context).collectAsState(initial = false)
    val enabledMessengerString by BTRMDataStore.getValueFlow(ENABLED_MESSENGERS, context).collectAsState(initial = "")
    val ttsModeSelection by BTRMDataStore.getValueFlow(TTS_MODE, context).collectAsState(initial = 0)
    val randomVoice by BTRMDataStore.getValueFlow(RANDOM_VOICE, context).collectAsState(initial = false)

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = settingsShown,
                        transitionSpec = { slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut() using
                                SizeTransform(clip = false)
                                         },
                        label = "Screen Header"
                    ) {
                        when (it) {
                            true -> { Text(stringResource(R.string.settings_header)) }
                            false -> { Text(stringResource(R.string.main_screen_header)) }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { settingsShown = !settingsShown } ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open/close settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = settingsShown,
            transitionSpec = { slideInHorizontally { -it } + fadeIn() togetherWith
                    slideOutHorizontally { it } + fadeOut() using
                    SizeTransform(clip = false)
            },
            label = "Screen Contents"
        ) {
            when (it) {
                true -> {
                    SettingsScreenLayout(
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
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                false -> {
                    MainScreenLayout(
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
                        onClickOpenMusic = onClickOpenMusic,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreenLayout(
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    ttsModeSelection: Int?,
    installedMessengers: ArrayList<MessengerApp>,
    enabledMessengerString: String?,
    installedMusicApps: ArrayList<MusicApp>,
    selectedMusicApp: MusicApp,
    randomVoice: Boolean?,
    postNotificationPermissionGranted: Boolean,
    readNotificationsPermissionGranted: Boolean,
    btStatusPermissionGranted: Boolean,
    btStatus: String,
    onSetTtsMode: (Int) -> Unit,
    onCheckedChangeMessengerApp: (String, Boolean) -> Unit,
    onSelectMusicApp: (MusicApp) -> Unit,
    onSetRandomVoice: (Boolean) -> Unit,
    onClickStopService: () -> Unit,
    onClickStartService: () -> Unit,
    onClickServiceStatus: () -> Unit,
    onClickRequestReadNotificationsPermission: () -> Unit,
    onClickRequestPostNotificationPermission: () -> Unit,
    onClickRequestBtPermission: () -> Unit,
    onClickAbandonAudiofocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val options = listOf(
        stringResource(R.string.mode_switch_allapps),
        stringResource(R.string.mode_switch_messengers)
    )

    // Используем фон, чтобы разделить карточки и создать ощущение глубины
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Секция переключения уведомлений по мессенджерам

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.mode_switch_header),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    options.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size
                            ),
                            onClick = { onSetTtsMode(index) },
                            selected = index == ttsModeSelection,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label, maxLines = 1)
                        }
                    }
                }

                if (installedMessengers.count() > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    MessengerAppColumn(
                        installedMessengerApps = installedMessengers,
                        enabledMessengerString = enabledMessengerString,
                        enabled = ttsModeSelection == 1,
                        onCheckedChangeMessengerApp = onCheckedChangeMessengerApp
                    )
                }
            }
        }

        if (installedMusicApps.count() > 0) {
            // Секция выбора музыкального приложения

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.music_app_header), style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    MusicAppRow(
                        installedMusicApps = installedMusicApps,
                        selectedMusicApp = selectedMusicApp,
                        onSelectMusicApp = onSelectMusicApp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Card(
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            // Секция включения случайного голоса
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.random_voice_header),
                    style = MaterialTheme.typography.headlineSmall, // Красивый заголовок
                    modifier = Modifier.padding(end = 12.dp)
                )
                Switch(
                    checked = randomVoice == true,
                    onCheckedChange = onSetRandomVoice
                )
            }
        }

        // Карточка для управления сервисом
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.service_header),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    MyButton(
                        text = stringResource(R.string.service_stop),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        onClickAction = onClickStopService,
                        icon = ImageVector.vectorResource(R.drawable.ic_stop) // Добавляем иконку
                    )
                    MyButton(
                        text = stringResource(R.string.service_info),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        onClickAction = {
                            onClickServiceStatus
                            if (!postNotificationPermissionGranted)

                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.no_post_notification_permission),
                                        actionLabel = context.getString(R.string.enable_permission),
                                        duration = SnackbarDuration.Long
                                    )
                                    when (result) {
                                        SnackbarResult.ActionPerformed -> onClickRequestPostNotificationPermission()
                                        SnackbarResult.Dismissed -> {}
                                    }
                                }
                        },
                        icon = Icons.Default.Info // Добавляем иконку
                    )
                    MyButton(
                        text = stringResource(R.string.service_start),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        onClickAction = {
                            onClickStartService
                            if (!readNotificationsPermissionGranted)
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
                        icon = Icons.Default.PlayArrow // Добавляем иконку
                    )
                }

            }
        }

        // Секция для статуса Bluetooth
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.bt_status),
                    style = MaterialTheme.typography.headlineSmall
                ) // Заголовок крупнее
                Spacer(modifier = Modifier.weight(1f))
                if (btStatusPermissionGranted)
                    Text(
                        text = btStatus,
                        style = MaterialTheme.typography.bodyLarge
                    )
                else
                    MyButton(
                        text = stringResource(R.string.enable_permission),
                        onClickAction = onClickRequestBtPermission
                    )
            }
        }

        // Секция кнопки сброса аудиофокуса

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            MyButton(
                text = stringResource(R.string.abandon_audiofocus),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClickAction = onClickAbandonAudiofocus,
                icon = ImageVector.vectorResource(R.drawable.volume_up) // Добавляем иконку
            )
        }
    }
}

@Composable
fun MainScreenLayout(
    testTextToSpeech: String,
    onTestTextToSpeechChange: (String) -> Unit,
    logMessages: String,
    useTTS: Boolean,
    selectedMusicApp: MusicApp,
    isReadingTestText: Boolean,
    onClickReadTestText: (String) -> Unit,
    onClickStopReading: () -> Unit,
    onClearLog: () -> Unit,
    onChangeUseTTS: (Boolean) -> Unit,
    onClickOpenMusic: (launchMusicAppIntent: Intent?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Используем фон, чтобы разделить карточки и создать ощущение глубины
    Column(modifier = modifier
        .background(MaterialTheme.colorScheme.background)
        .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Секция для проверки работы TTS
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = testTextToSpeech,
                    onValueChange = { onTestTextToSpeechChange(it) },
                    label = {
                        Text(text = stringResource(R.string.tts_test_label))
                    },
                    trailingIcon = {
                        Image(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.clickable { onTestTextToSpeechChange("") }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isReadingTestText)
                // Кнопка остановки чтения текста
                    MyButton(
                        text = stringResource(R.string.tts_stop),
                        onClickAction = { onClickStopReading() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        icon = ImageVector.vectorResource(R.drawable.ic_close)
                    )
                else
                    // Кнопка чтения текста
                    MyButton(
                        text = stringResource(R.string.tts_read),
                        onClickAction = { onClickReadTestText(testTextToSpeech) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        icon = ImageVector.vectorResource(R.drawable.ic_music_note)
                    )
            }
        }

        // Секция для логов
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.log_messages),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = logMessages,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                )
                // Кнопка очистки логов
                MyButton(
                    text = stringResource(R.string.log_messages_clear),
                    onClickAction = onClearLog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    icon = Icons.Default.Delete // Иконка удаления
                )
            }
        }

        Card(
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            // Переключатель TTS
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(vertical = 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.text_to_speech),
                    style = MaterialTheme.typography.headlineMedium, // Красивый заголовок
                    modifier = Modifier.padding(end = 12.dp)
                )
                Switch(
                    checked = useTTS,
                    onCheckedChange = onChangeUseTTS
                )
            }
        }

        // Кнопка открытия приложения
        Card(
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.music_header),
                    style = MaterialTheme.typography.headlineSmall, // Красивый заголовок
                    modifier = Modifier.padding(end = 12.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                MyButton(
                    text = stringResource(R.string.music_open, selectedMusicApp.name),
                    onClickAction = { onClickOpenMusic(selectedMusicApp.launchIntent) },
                    modifier = Modifier.weight(2f),
                    enabled = selectedMusicApp.launchIntent != null,
                    icon = ImageVector.vectorResource(R.drawable.ic_music_note)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsLayoutPreview() {
    BtReaderMusicTheme {
        SettingsScreenLayout(
            scope = rememberCoroutineScope(),
            snackbarHostState = SnackbarHostState(),
            ttsModeSelection = 1,
            installedMessengers = arrayListOf(
                MessengerApp("org.whatsapp", "Whatsapp", null),
                MessengerApp("org.telegram.messenger", "Telegram", null),
            ),
            enabledMessengerString = null,
            installedMusicApps = arrayListOf(
                MusicApp("ru.yandex.music", null, "Яндекс Музыка", null),
                MusicApp("com.spotify.music", null, "Spotify", null),
                MusicApp("com.google.android.apps.youtube.music", null, "Youtube Music", null)
            ),
            randomVoice = false,
            postNotificationPermissionGranted = false,
            readNotificationsPermissionGranted = false,
            btStatusPermissionGranted = true,
            btStatus = "CONNECTED",
            selectedMusicApp = MusicApp("", null, "", null),
            onSetTtsMode = {},
            onCheckedChangeMessengerApp = { _, _ -> },
            onSelectMusicApp = {},
            onSetRandomVoice = {},
            onClickStopService = {},
            onClickStartService = {},
            onClickServiceStatus = {},
            onClickRequestReadNotificationsPermission = {},
            onClickRequestPostNotificationPermission = {},
            onClickRequestBtPermission = {},
            onClickAbandonAudiofocus = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainLayoutPreview() {
    BtReaderMusicTheme {
        MainScreenLayout(
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

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    BtReaderMusicTheme {
        MainScreen(
            viewModel = MainViewModel(),
            onClickReadTestText = {},
            onClickStopReading = {},
            onClickStopService = {},
            onClickStartService = {},
            onClickServiceStatus = {},
            onSelectMusicApp = {},
            onChangeUseTTS = {},
            onSetTtsMode = {},
            onCheckedChangeMessengerApp = { _, _ -> },
            onClickRequestReadNotificationsPermission = {},
            onClickRequestPostNotificationPermission = {},
            onClickRequestBtPermission = {},
            onClickAbandonAudiofocus = {},
            onSetRandomVoice = {},
            onClickOpenMusic = {}
        )
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun SettingsLayoutPreviewInRussian() {
    BtReaderMusicTheme {
        SettingsScreenLayout(
            scope = rememberCoroutineScope(),
            snackbarHostState = SnackbarHostState(),
            ttsModeSelection = 1,
            installedMessengers = arrayListOf(
                MessengerApp("org.whatsapp", "Whatsapp", null),
                MessengerApp("org.telegram.messenger", "Telegram", null),
            ),
            enabledMessengerString = null,
            installedMusicApps = arrayListOf(
                MusicApp("ru.yandex.music", null, "Яндекс Музыка", null),
                MusicApp("com.spotify.music", null, "Spotify", null),
                MusicApp("com.google.android.apps.youtube.music", null, "Youtube Music", null)
            ),
            randomVoice = false,
            btStatusPermissionGranted = true,
            btStatus = "CONNECTED",
            selectedMusicApp = MusicApp("", null, "", null),
            onSetTtsMode = {},
            onCheckedChangeMessengerApp = { _, _ -> },
            onSelectMusicApp = {},
            postNotificationPermissionGranted = false,
            readNotificationsPermissionGranted = false,
            onSetRandomVoice = {},
            onClickStopService = {},
            onClickStartService = {},
            onClickServiceStatus = {},
            onClickRequestReadNotificationsPermission = {},
            onClickRequestPostNotificationPermission = {},
            onClickRequestBtPermission = {},
            onClickAbandonAudiofocus = {}
        )
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun MainScreenPreviewInRussian() {
    BtReaderMusicTheme {
        MainScreen(
            viewModel = MainViewModel(),
            onClickReadTestText = {},
            onClickStopReading = {},
            onClickStopService = {},
            onClickStartService = {},
            onClickServiceStatus = {},
            onSelectMusicApp = {},
            onChangeUseTTS = {},
            onSetTtsMode = {},
            onCheckedChangeMessengerApp = { _, _ -> },
            onClickRequestReadNotificationsPermission = {},
            onClickRequestPostNotificationPermission = {},
            onClickRequestBtPermission = {},
            onClickAbandonAudiofocus = {},
            onSetRandomVoice = {},
            onClickOpenMusic = {}
        )
    }
}

@Composable
fun MyButton(
    text: String,
    onClickAction: () -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    ElevatedButton(
        onClick = onClickAction,
        contentPadding = PaddingValues(),
        enabled = enabled,
        modifier = modifier.defaultMinSize(
            minWidth = ButtonDefaults.MinWidth
        )
    ) {
        Row {
            icon?.let {
                Icon(
                    imageVector = icon,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(4.dp))
            }
            Text(
                text = text,
                maxLines = 1,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun MusicAppRow(
    installedMusicApps: ArrayList<MusicApp>,
    selectedMusicApp: MusicApp,
    onSelectMusicApp: (MusicApp) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(8.dp)) {
        LazyRow( // Горизонтальный скролл для приложений
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(installedMusicApps) { musicApp ->
                MusicAppCard(
                    musicApp = musicApp,
                    isSelected = selectedMusicApp == musicApp, // Выделение выбранного приложения
                    onSelectMusicApp = { onSelectMusicApp(musicApp) }
                )
            }
        }
    }
}

@Composable
fun MusicAppCard(
    musicApp: MusicApp,
    isSelected: Boolean,
    onSelectMusicApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .size(60.dp)
            .clickable(onClick = onSelectMusicApp)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(4.dp),
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = rememberDrawablePainter(drawable = musicApp.icon),
                contentDescription = "${musicApp.name} icon",
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = musicApp.name,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MessengerAppColumn(
    installedMessengerApps: ArrayList<MessengerApp>,
    enabledMessengerString: String?,
    enabled: Boolean,
    onCheckedChangeMessengerApp: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Messengers",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.alpha(if (!enabled) 0.5f else 1f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column( // Вертикальный скролл для приложений
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            installedMessengerApps.forEach { messengerApp ->
                MessengerAppCard(
                    messengerApp = messengerApp,
                    enabledMessengerString = enabledMessengerString,
                    enabled = enabled,
                    onCheckedChangeMessengerApp = onCheckedChangeMessengerApp
                )
            }
        }
    }
}

@Composable
fun MessengerAppCard(
    messengerApp: MessengerApp,
    enabledMessengerString: String?,
    enabled: Boolean,
    onCheckedChangeMessengerApp: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(4.dp),
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = rememberDrawablePainter(drawable = messengerApp.icon),
                contentDescription = "${messengerApp.name} icon",
                modifier = Modifier
                    .alpha(if (!enabled) 0.5f else 1f)
                    .size(48.dp)
                    .padding(6.dp)
            )
            Text(
                text = messengerApp.name,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(if (!enabled) 0.5f else 1f)
                    .padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = enabledMessengerString?.getList()?.contains(messengerApp.packageName) == true,
                onCheckedChange = { checked: Boolean ->
                    onCheckedChangeMessengerApp(messengerApp.packageName, checked)
                },
                enabled = enabled,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    }
}