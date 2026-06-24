package com.katdmy.android.bluetoothreadermusic.ui.vm

import androidx.lifecycle.ViewModel
import com.katdmy.android.bluetoothreadermusic.data.enums.AudioFocusMode
import com.katdmy.android.bluetoothreadermusic.data.enums.NotificationPart
import com.katdmy.android.bluetoothreadermusic.data.models.AppVoiceSettings
import com.katdmy.android.bluetoothreadermusic.data.models.MainUiModel
import com.katdmy.android.bluetoothreadermusic.data.models.InstalledApp
import com.katdmy.android.bluetoothreadermusic.data.models.GrantedPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiModel())
    val uiState: StateFlow<MainUiModel> = _uiState.asStateFlow()

    private val _permissionState = MutableStateFlow(GrantedPermissions())
    val permissionState: StateFlow<GrantedPermissions> = _permissionState.asStateFlow()

    init {
        onSetReadingTestText(false)
    }

    fun onSetInstalledApps(newInstalledApps: List<InstalledApp>) {
        _uiState.update { it.copy(installedApps = newInstalledApps) }
    }

    fun onSetAddedApps(newAddedApps: List<InstalledApp>) {
        _uiState.update { it.copy(addedApps = newAddedApps) }
    }

    fun onSetVoicesCount(newVoicesCount: Int) {
        _uiState.update { it.copy(voicesCount = newVoicesCount) }
    }

    fun onSetReadNotificationsPermission(newReadNotificationsPermission: Boolean) {
        _permissionState.update { it.copy(readNotifications = newReadNotificationsPermission)}
    }

    fun onSetReadingTestText(newReadingTestText: Boolean) {
        _uiState.update { it.copy(isReadingTestText = newReadingTestText) }
    }

    fun onSetPostNotificationPermission(newPostNotificationPermission: Boolean) {
        _permissionState.update { it.copy(postNotification = newPostNotificationPermission) }
    }

    fun onSetBTStatusPermission(newBTStatusPermission: Boolean) {
        _permissionState.update { it.copy(btStatus = newBTStatusPermission) }
    }

    fun onSetGlobalAudioFocusMode(newMode: AudioFocusMode) {
        _uiState.update { it.copy(globalAudioFocusMode = newMode) }
    }

    fun onSetGlobalNotificationParts(newParts: Set<NotificationPart>) {
        _uiState.update { it.copy(globalNotificationParts = newParts) }
    }

    fun onSetAllAppSettings(newSettings: List<AppVoiceSettings>) {
        _uiState.update { it.copy(allAppSettings = newSettings) }
    }

    fun onSetReadUpdates(newReadUpdates: Boolean) {
        _uiState.update { it.copy(readUpdates = newReadUpdates) }
    }

    fun onSetUseTTS(newUseTTS: Boolean) {
        _uiState.update { it.copy(useTTS = newUseTTS) }
    }

    fun onSeTtsModeSelection(newTtsModeSelection: Int) {
        _uiState.update { it.copy(ttsModeSelection = newTtsModeSelection) }
    }

    fun onSetRandomVoice(newRandomVoice: Boolean) {
        _uiState.update { it.copy(randomVoice = newRandomVoice) }
    }

    fun onSetTtsVolume(newTtsVolume: Float) {
        _uiState.update { it.copy(ttsVolume = newTtsVolume) }
    }

    fun onSetShowLog(newShowLog: Boolean) {
        _uiState.update { it.copy(showLog = newShowLog) }
    }
}