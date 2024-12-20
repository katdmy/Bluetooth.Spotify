package com.katdmy.android.bluetoothreadermusic.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
//import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.util.Constants.MUSIC_PACKAGE_NAME
import com.katdmy.android.bluetoothreadermusic.util.Constants.USE_TTS_SF
import com.katdmy.android.bluetoothreadermusic.data.*
import com.katdmy.android.bluetoothreadermusic.receivers.BtBroadcastReceiver
import com.katdmy.android.bluetoothreadermusic.receivers.NotificationBroadcastReceiver
import com.katdmy.android.bluetoothreadermusic.services.NotificationListener
import com.katdmy.android.bluetoothreadermusic.ui.theme.BtReaderMusicTheme
import com.katdmy.android.bluetoothreadermusic.util.BTRMDataStore
import com.katdmy.android.bluetoothreadermusic.util.BluetoothConnectionChecker
import com.katdmy.android.bluetoothreadermusic.util.StringListHelper.getList
import com.katdmy.android.bluetoothreadermusic.util.StringListHelper.getString
import com.katdmy.android.bluetoothreadermusic.util.Constants.ENABLED_MESSENGERS
import com.katdmy.android.bluetoothreadermusic.util.Constants.RANDOM_VOICE
import com.katdmy.android.bluetoothreadermusic.util.Constants.SERVICE_STARTED
import com.katdmy.android.bluetoothreadermusic.util.Constants.TTS_MODE
import kotlinx.coroutines.launch

class ComposeActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel>()
    private lateinit var notificationBroadcastReceiver: NotificationBroadcastReceiver
    private lateinit var btBroadcastReceiver: BtBroadcastReceiver
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

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

        initMusicApps(viewModel::onSetInstalledMusicApps)
        initMessengerApps(viewModel::onSetInstalledMessengerApps)
        requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()) { permissionAndGrant ->
            if (permissionAndGrant.values.contains(false)) {
                permissionAndGrant.forEach { (name: String, isGranted: Boolean) ->
                    if (!isGranted) showNoPermissionDialog(name)
                }
            } else {
                getInitialBluetoothStatus(viewModel::onChangeBtStatus)
            }
        }
        registerReceivers()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermissions()
        }
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

                if (!isNotificationServiceRunning()) {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            }
        }

        setContent {
            BtReaderMusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        viewModel,
                        ::onClickStopService,
                        ::onClickStartService,
                        ::onClickServiceStatus,
                        ::onSelectMusicApp,
                        ::onChangeUseTTS,
                        ::onSetTtsMode,
                        ::onCheckedChangeMessengerApp,
                        ::onSetRandomVoice,
                        ::onClickAbandonAudiofocus,
                        ::onClickOpenMusic
                    )
                }
            }
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
                    ))
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

        if (Settings.Secure.getString(this.contentResolver, "enabled_notification_listeners")
                .contains(applicationContext.packageName)
        )
            Intent(this, NotificationListener::class.java).also { intent -> startService(intent) }
        else
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkPermissions() {
        val postNotificationPermissionGranted = when {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ->
                true
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.POST_NOTIFICATIONS) -> {
                // explain to the user why your app requires this permission and what features are disabled if it's declined
                // include "cancel" button that lets the user continue without granting the permission
                showRequestPermissionDialog()
                true
            }
            else -> {
                false
            }
        }
        val bluetoothConnectPermissionGranted = when {
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED -> {
                true
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.BLUETOOTH_CONNECT) -> {
                // explain to the user why your app requires this permission and what features are disabled if it's declined
                // include "cancel" button that lets the user continue without granting the permission
                showRequestPermissionDialog()
                true
            }
            else -> {
                false
            }
        }
        if (!bluetoothConnectPermissionGranted && !postNotificationPermissionGranted)
        // directly ask for the permission, registered ActivityResultCallback gets the result
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.BLUETOOTH_CONNECT
            ))
        else {
            if (!bluetoothConnectPermissionGranted)
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
            if (!postNotificationPermissionGranted)
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            else {
                getInitialBluetoothStatus(viewModel::onChangeBtStatus)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun showRequestPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_header))
            .setMessage(getString(R.string.permission_complete_rationale))
            .setPositiveButton(getString(R.string.permission_enable)) { _, _ -> requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.BLUETOOTH_CONNECT
            )) }
            .setNegativeButton(getString(R.string.permission_disable)) { _, _ -> }
            .create()
            .show()
    }

    @SuppressLint("StringFormatInvalid")
    private fun showNoPermissionDialog(name: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_required_header))
            .setMessage(getString(R.string.permission_required_header, name))
            .create()
            .show()
    }

    private fun isNotificationServiceRunning(): Boolean {
        val enabledNotificationListeners =
            Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabledNotificationListeners != null && enabledNotificationListeners.contains(
            packageName
        )
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onClickStopService: () -> Unit,
    onClickStartService: () -> Unit,
    onClickServiceStatus: () -> Unit,
    onSelectMusicApp: (MusicApp) -> Unit,
    onChangeUseTTS: (Boolean) -> Unit,
    onSetTtsMode: (Int) -> Unit,
    onCheckedChangeMessengerApp: (String, Boolean) -> Unit,
    onSetRandomVoice: (Boolean) -> Unit,
    onClickAbandonAudiofocus: () -> Unit,
    onClickOpenMusic: (launchMusicAppIntent: Intent?) -> Unit
) {
    val state = viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val useTTS by BTRMDataStore.getValueFlow(USE_TTS_SF, context).collectAsState(initial = false)
    val enabledMessengerString by BTRMDataStore.getValueFlow(ENABLED_MESSENGERS, context).collectAsState(initial = "")
    val ttsModeSelection by BTRMDataStore.getValueFlow(TTS_MODE, context).collectAsState(initial = 0)
    val randomVoice by BTRMDataStore.getValueFlow(RANDOM_VOICE, context).collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = state.value.settingsShown,
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
                    IconButton(onClick = viewModel::toggleSettingsShown ) {
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
            targetState = state.value.settingsShown,
            transitionSpec = { slideInHorizontally { -it } + fadeIn() togetherWith
                    slideOutHorizontally { it } + fadeOut() using
                    SizeTransform(clip = false)
            },
            label = "Screen Contents"
        ) {
            when (it) {
                true -> {
                    SettingsScreenLayout(
                        ttsModeSelection = ttsModeSelection,
                        installedMessengers = state.value.installedMessengerApps,
                        enabledMessengerString = enabledMessengerString,
                        installedMusicApps = state.value.installedMusicApps,
                        selectedMusicApp = state.value.selectedMusicApp,
                        randomVoice = randomVoice,
                        onSetTtsMode = onSetTtsMode,
                        onCheckedChangeMessengerApp = onCheckedChangeMessengerApp,
                        onSelectMusicApp = onSelectMusicApp,
                        onSetRandomVoice = onSetRandomVoice,
                        onClickAbandonAudiofocus = onClickAbandonAudiofocus,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                false -> {
                    MainScreenLayout(
                        btStatus = state.value.btStatus,
                        logMessages = state.value.logMessages,
                        selectedMusicApp = state.value.selectedMusicApp,
                        useTTS = useTTS == true,
                        onClearLog = viewModel::onClearLogMessages,
                        onClickStopService = onClickStopService,
                        onClickStartService = onClickStartService,
                        onClickServiceStatus = onClickServiceStatus,
                        onChangeUseTTS = onChangeUseTTS,
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
    ttsModeSelection: Int?,
    installedMessengers: ArrayList<MessengerApp>,
    enabledMessengerString: String?,
    installedMusicApps: ArrayList<MusicApp>,
    selectedMusicApp: MusicApp,
    randomVoice: Boolean?,
    onSetTtsMode: (Int) -> Unit,
    onCheckedChangeMessengerApp: (String, Boolean) -> Unit,
    onSelectMusicApp: (MusicApp) -> Unit,
    onSetRandomVoice: (Boolean) -> Unit,
    onClickAbandonAudiofocus: () -> Unit,
    modifier: Modifier = Modifier
) {
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
    btStatus: String,
    logMessages: String,
    useTTS: Boolean,
    selectedMusicApp: MusicApp,
    onClearLog: () -> Unit,
    onClickStopService: () -> Unit,
    onClickStartService: () -> Unit,
    onClickServiceStatus: () -> Unit,
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
                        onClickAction = onClickServiceStatus,
                        icon = Icons.Default.Info // Добавляем иконку
                    )
                    MyButton(
                        text = stringResource(R.string.service_start),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        onClickAction = onClickStartService,
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
                Text(
                    text = btStatus,
                    style = MaterialTheme.typography.bodyLarge
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
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
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
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.text_to_speech),
                    style = MaterialTheme.typography.headlineSmall, // Красивый заголовок
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
            selectedMusicApp = MusicApp("", null, "", null),
            onSetTtsMode = {},
            onCheckedChangeMessengerApp = { _, _ -> },
            onSelectMusicApp = {},
            onSetRandomVoice = {},
            onClickAbandonAudiofocus = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainLayoutPreview() {
    BtReaderMusicTheme {
        MainScreenLayout(
            btStatus = "CONNECTED",
            logMessages = "",
            useTTS = false,
            selectedMusicApp = MusicApp("com.spotify.music", null, "Spotify", null),
            onClearLog = {},
            onClickStopService = {},
            onClickStartService = {},
            onClickServiceStatus = {},
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
            onClickStopService = {},
            onClickStartService = {},
            onClickServiceStatus = {},
            onSelectMusicApp = {},
            onChangeUseTTS = {},
            onSetTtsMode = {},
            onCheckedChangeMessengerApp = { _, _ -> },
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
            selectedMusicApp = MusicApp("", null, "", null),
            onSetTtsMode = {},
            onCheckedChangeMessengerApp = { _, _ -> },
            onSelectMusicApp = {},
            onSetRandomVoice = {},
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
            onClickStopService = {},
            onClickStartService = {},
            onClickServiceStatus = {},
            onSelectMusicApp = {},
            onChangeUseTTS = {},
            onSetTtsMode = {},
            onCheckedChangeMessengerApp = { _, _ -> },
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
    icon: ImageVector,
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
            Icon(
                imageVector = icon,
                contentDescription = null
            )
            Spacer(modifier = Modifier.size(4.dp))
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
                modifier = Modifier.alpha(if (!enabled) 0.5f else 1f).size(48.dp).padding(6.dp)
            )
            Text(
                text = messengerApp.name,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(if (!enabled) 0.5f else 1f).padding(start = 4.dp)
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