package com.katdmy.android.bluetoothreadermusic.ui.views

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.data.Navigation
import com.katdmy.android.bluetoothreadermusic.data.ServiceStatus
import com.katdmy.android.bluetoothreadermusic.data.models.InstalledApp
import com.katdmy.android.bluetoothreadermusic.services.StatusService
import com.katdmy.android.bluetoothreadermusic.ui.theme.BtReaderMusicTheme
import com.katdmy.android.bluetoothreadermusic.ui.vm.MainViewModel
import com.katdmy.android.bluetoothreadermusic.util.BTRMDataStore
import com.katdmy.android.bluetoothreadermusic.util.Constants.RANDOM_VOICE
import com.katdmy.android.bluetoothreadermusic.util.Constants.TTS_MODE
import com.katdmy.android.bluetoothreadermusic.util.Constants.USE_TTS_SF
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun BTReaderApp(
    viewModel: MainViewModel,
    onGetInstalledLaunchableApps: () -> List<InstalledApp>,
    onClickReadTestText: (String) -> Unit,
    restartNotificationListener: () -> Unit,
    onClickStopReading: () -> Unit,
    onClickDeleteApp: (String) -> Unit,
    onAddApps: (List<String>) -> Unit,
    onChangeUseTTS: (Boolean) -> Unit,
    onSetTtsMode: (Int) -> Unit,
    onSetRandomVoice: (Boolean) -> Unit,
    onClickRequestReadNotificationsPermission: () -> Unit,
    onClickRequestPostNotificationPermission: () -> Unit,
    onClickRequestBtPermission: () -> Unit,
    onClickForceRestartTTS: () -> Unit,
    onClickOpenTTSSettings: () -> Unit,
    modifier: Modifier = Modifier
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
    val ttsModeSelection by BTRMDataStore.getValueFlow(TTS_MODE, context).collectAsState(initial = 0)
    val randomVoice by BTRMDataStore.getValueFlow(RANDOM_VOICE, context).collectAsState(initial = false)

    val serviceHealth by StatusService.serviceHealth.collectAsState()
    var badgeNeeded by remember { mutableStateOf(false) }
    if (!permissions.value.postNotification ||
        !permissions.value.readNotifications ||
        serviceHealth == ServiceStatus.Dead)
            badgeNeeded = true
    else
            badgeNeeded = false

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = modifier,
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
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = "Back"
                            )
                        }
                },
                actions = {
                    if (navigation == Navigation.Main)
                        IconButton(onClick = { navigation = Navigation.SettingsScreen } ) {
                            BadgedBox(badge = {
                                    if (badgeNeeded) { Badge() }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_settings),
                                    contentDescription = "Open/close settings"
                                )
                            }
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
                        ttsModeSelection = ttsModeSelection,
                        addedApps = state.value.addedApps,
                        randomVoice = randomVoice,
                        voicesCount = state.value.voicesCount,
                        postNotificationPermissionGranted = permissions.value.postNotification,
                        readNotificationsPermissionGranted = permissions.value.readNotifications,
                        btStatusPermissionGranted = permissions.value.btStatus,
                        btStatus = state.value.btStatus,
                        onGetInstalledLaunchableApps = onGetInstalledLaunchableApps,
                        onSetTtsMode = onSetTtsMode,
                        onClickDeleteApp = onClickDeleteApp,
                        onClickAddApp = onAddApps,
                        onSetRandomVoice = onSetRandomVoice,
                        onClickOpenTTSSettings = onClickOpenTTSSettings,
                        openNotificationSettings = restartNotificationListener,
                        onClickRequestReadNotificationsPermission = onClickRequestReadNotificationsPermission,
                        onClickRequestPostNotificationPermission = onClickRequestPostNotificationPermission,
                        onClickRequestBtPermission = onClickRequestBtPermission,
                        onClickForceRestartTTS = onClickForceRestartTTS,
                        onClickPrivacyPolicy = { navigation = Navigation.PrivacyPolicyScreeen }
                    )
                }
                Navigation.Main -> {
                    val noReadNotificationPermission = stringResource(R.string.no_read_notifications_permission)
                    val enablePermission = stringResource(R.string.enable_permission)
                    MainScreen(
                        testTextToSpeech = testTextToSpeech,
                        onTestTextToSpeechChange = { newText -> testTextToSpeech = newText },
                        logMessages = state.value.logMessages,
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
                                        message = noReadNotificationPermission,
                                        actionLabel = enablePermission,
                                        duration = SnackbarDuration.Long
                                    )
                                    when (result) {
                                        SnackbarResult.ActionPerformed -> onClickRequestReadNotificationsPermission()
                                        SnackbarResult.Dismissed -> {}
                                    }
                                }
                        }
                    )
                }
                Navigation.PrivacyPolicyScreeen -> {
                    PrivacyPolicyScreeen(
                        stringResource(R.string.privacy_policy_url)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BTReaderAppPreview() {
    BtReaderMusicTheme {
        MainScreen(
            testTextToSpeech = "",
            onTestTextToSpeechChange = {},
            logMessages = "",
            useTTS = false,
            isReadingTestText = false,
            onClickReadTestText = {},
            onClickStopReading = {},
            onClearLog = {},
            onChangeUseTTS = {}
        )
    }
}