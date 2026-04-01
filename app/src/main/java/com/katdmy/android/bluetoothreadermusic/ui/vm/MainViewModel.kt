package com.katdmy.android.bluetoothreadermusic.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katdmy.android.bluetoothreadermusic.data.models.MainUiModel
import com.katdmy.android.bluetoothreadermusic.data.models.InstalledApp
import com.katdmy.android.bluetoothreadermusic.data.models.GrantedPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiModel())
    val uiState: StateFlow<MainUiModel> = _uiState.asStateFlow()

    private val _isReadingTestText = MutableStateFlow(false)
    val isReadingTestText: StateFlow<Boolean> = _isReadingTestText.asStateFlow()

    private val _permissionState = MutableStateFlow(GrantedPermissions())
    val permissionState: StateFlow<GrantedPermissions> = _permissionState.asStateFlow()

    init {
        onSetReadingTestText(false)
    }

    fun onSetInstalledApps(newInstalledApps: List<InstalledApp>) {
        viewModelScope.launch {
            _uiState.emit(_uiState.value.copy(addedApps = newInstalledApps))
        }
    }

    fun onAddLogMessage(log: String) {
        viewModelScope.launch {
            val newLog = uiState.value.logMessages + "\n\n" + log
            _uiState.emit(_uiState.value.copy(logMessages = newLog))
        }
    }

    fun onClearLogMessages() {
        viewModelScope.launch {
            _uiState.emit(_uiState.value.copy(logMessages = ""))
        }
    }

    fun onChangeBtStatus(newBtStatus: String) {
        viewModelScope.launch {
            _uiState.emit(_uiState.value.copy(btStatus = newBtStatus))
        }
    }

    fun onSetReadNotificationsPermission(newReadNotificationsPermission: Boolean) {
        viewModelScope.launch {
            _permissionState.emit(_permissionState.value.copy(readNotifications = newReadNotificationsPermission))
        }
    }

    fun onSetReadingTestText(newReadingTestText: Boolean) {
        viewModelScope.launch {
            _isReadingTestText.emit(newReadingTestText)
        }
    }

    fun onSetPostNotificationPermission(newPostNotificationPermission: Boolean) {
        viewModelScope.launch {
            _permissionState.emit(_permissionState.value.copy(postNotification = newPostNotificationPermission))
        }
    }

    fun onSetBTStatusPermission(newBTStatusPermission: Boolean) {
        viewModelScope.launch {
            _permissionState.emit(_permissionState.value.copy(btStatus = newBTStatusPermission))
        }
    }
}