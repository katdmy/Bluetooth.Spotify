package com.katdmy.android.bluetoothreadermusic.presentation

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
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.katdmy.android.bluetoothreadermusic.data.MusicApp
import com.katdmy.android.bluetoothreadermusic.receivers.BtBroadcastReceiver
import com.katdmy.android.bluetoothreadermusic.receivers.NotificationBroadcastReceiver
import com.katdmy.android.bluetoothreadermusic.services.NotificationListener
import com.katdmy.android.bluetoothreadermusic.ui.theme.BtReaderMusicTheme

class ComposeActivity : ComponentActivity() {

    private val composeViewModel by viewModels<ComposeViewModel>()
    private lateinit var notificationBroadcastReceiver: NotificationBroadcastReceiver
    private lateinit var btBroadcastReceiver: BtBroadcastReceiver
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationBroadcastReceiver = NotificationBroadcastReceiver(
            changeUseTTS = composeViewModel::onChangeUseTTS,
            addLogRecord = composeViewModel::onAddLogMessage
        )
        btBroadcastReceiver = BtBroadcastReceiver(
            changeUseTTS = composeViewModel::onChangeUseTTS,
            changeConnectionStatus = composeViewModel::onChangeBtStatus
        )

        initMusicApps(composeViewModel::onSetInstalledMusicApps)
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionAndGrant ->
            if (permissionAndGrant.values.contains(false)) {
                permissionAndGrant.forEach { (name: String, isGranted: Boolean) -> if (!isGranted) showNoPermissionDialog(name) }
            }
        }
        registerReceivers()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermissions()
        }

        setContent {
            BtReaderMusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        composeViewModel,
                        ::onClickStopService,
                        ::onClickStartService,
                        ::onChangeUseTTS,
                        ::onClickOpenMusic)
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
                val ai = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getApplicationInfo(
                        app,
                        PackageManager.ApplicationInfoFlags.of(0)
                    )
                } else {
                    packageManager.getApplicationInfo(app, 0)
                }
                val name = packageManager.getApplicationLabel(ai).toString()
                val launchIntent = packageManager.getLaunchIntentForPackage(app)
                val icon = packageManager.getApplicationIcon(app)
                installedMusicApps.add(MusicApp(app, launchIntent, name, icon))
            }
        }
        setToModel(installedMusicApps)
        Log.e("InstalledMusicApps", installedMusicApps.toString())
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

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceivers() {
        if (Settings.Secure.getString(this.contentResolver, "enabled_notification_listeners")
                .contains(applicationContext.packageName)
        ) {
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

            Intent(this, NotificationListener::class.java).also { intent -> startService(intent) }
        } else
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
    }

    private fun onChangeUseTTS(useTTS: Boolean) {
        /*val editor = sharedPreferences.edit()
        editor.putBoolean(Constants.USE_TTS_SF, isChecked)
        editor.apply()*/
        composeViewModel.onChangeUseTTS(useTTS)
        val intent =
            Intent("com.katdmy.android.bluetoothreadermusic.onVoiceUseChange")
        intent.putExtra("useTTS", useTTS)
        sendBroadcast(intent)
    }
    private fun onClickOpenMusic(launchMusicAppIntent: Intent?) {
        launchMusicAppIntent?.let { startActivity(it) }
    }
}

@Composable
fun MainScreen(
    viewModel: ComposeViewModel,
    onClickStopService: () -> Unit,
    onClickStartService: () -> Unit,
    onChangeUseTTS: (Boolean) -> Unit,
    onClickOpenMusic: (launchMusicAppIntent: Intent?) -> Unit
) {
    val state = viewModel.uiState.collectAsState()
    MainScreenLayout(
        btStatus = state.value.btStatus,
        logMessages = state.value.logMessages,
        useTTS = state.value.useTTS,
        installedMusicApps = state.value.installedMusicApps,
        selectedMusicApp = state.value.selectedMusicApp,
        onClearLog = viewModel::onClearLogMessages,
        onClickStopService = onClickStopService,
        onClickStartService = onClickStartService,
        onSelectMusicApp = viewModel::onSelectMusicApp,
        onChangeUseTTS = onChangeUseTTS,
        onClickOpenMusic = onClickOpenMusic
    )
}

@Composable
fun MainScreenLayout(
    btStatus: String,
    logMessages: String,
    useTTS: Boolean,
    installedMusicApps: ArrayList<MusicApp>,
    selectedMusicApp: MusicApp,
    onClearLog: () -> Unit,
    onClickStopService: () -> Unit,
    onClickStartService: () -> Unit,
    onSelectMusicApp: (MusicApp) -> Unit,
    onChangeUseTTS: (Boolean) -> Unit,
    onClickOpenMusic: (launchMusicAppIntent: Intent?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row {
            MyButton(text = "Stop Service", modifier = Modifier
                .padding(end = 8.dp)
                .weight(1f), onClickAction = onClickStopService)
            MyButton(text = "Start Service", modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f), onClickAction = onClickStartService)
        }
        Text(text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("BT status: ")
                }
                append(btStatus)
            },
            modifier = Modifier.fillMaxWidth())
        Text(text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Log messages: \n")
                }
                append(logMessages)
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth())
        MyButton(
            text = "Clear Log",
            modifier = Modifier.fillMaxWidth(),
            onClickAction = onClearLog )
        MusicAppRow(
            installedMusicApps = installedMusicApps,
            selectedMusicApp = selectedMusicApp,
            onSelectMusicApp = onSelectMusicApp,
            modifier = Modifier.fillMaxWidth())
        Row {
            MyButton(
                text = "Open Music App",
                onClickAction = {onClickOpenMusic(selectedMusicApp.launchIntent)},
                modifier = Modifier
                    .padding(end = 8.dp)
                    .weight(1f),
                enabled = selectedMusicApp.launchIntent != null
            )
            Row(
                Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "Text-to-Voice", modifier = Modifier.padding(horizontal = 8.dp))
                Switch(
                    checked = useTTS,
                    onCheckedChange = {
                        onChangeUseTTS(it)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainLayoutPreview() {
    BtReaderMusicTheme {
        MainScreenLayout(
            btStatus = "",
            logMessages = "",
            useTTS = false,
            installedMusicApps = arrayListOf(),
            selectedMusicApp = MusicApp("", null, "", null),
            onClearLog = {},
            onClickStopService = {},
            onClickStartService = {},
            onSelectMusicApp = {},
            onChangeUseTTS = {},
            onClickOpenMusic = {}
        )
    }
}

@Composable
fun MyButton(
    text: String,
    onClickAction: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(onClick = onClickAction, modifier = modifier, enabled=enabled) {
        Text(text = text, modifier = Modifier.align(Alignment.CenterVertically))
    }
}

@Composable
fun MusicAppRow(
    installedMusicApps: ArrayList<MusicApp>,
    selectedMusicApp: MusicApp,
    onSelectMusicApp: (MusicApp) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        Text(text = "Music App", modifier = Modifier
            .padding(end = 8.dp)
            .align(Alignment.CenterVertically))
        MusicAppSelection(
            selectedMusicAppName = selectedMusicApp.name,
            installedMusicApps = installedMusicApps,
            onSelectMusicApp = onSelectMusicApp,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        )
        Image(
            painter = rememberDrawablePainter(drawable = selectedMusicApp.icon),
            contentDescription = "music app icon",
            modifier = Modifier
                .size(48.dp)
                .padding(start = 8.dp)
                .align(Alignment.CenterVertically)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicAppSelection(
    selectedMusicAppName: String,
    installedMusicApps: ArrayList<MusicApp>,
    onSelectMusicApp: (MusicApp) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val onExpandedChange = { expanded = !expanded }

    Box(modifier = modifier) {
        OutlinedTextField(
            enabled = false,
            value = selectedMusicAppName,
            onValueChange = { },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            modifier = Modifier.clickable { onExpandedChange() }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            installedMusicApps.forEach { musicApp ->
                DropdownMenuItem(
                    onClick = {
                        onSelectMusicApp(musicApp)
                        expanded = false
                    },
                    text = { Text(text = musicApp.name) })
            }
        }
    }
}