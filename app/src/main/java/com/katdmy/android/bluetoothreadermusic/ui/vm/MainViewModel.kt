package com.katdmy.android.bluetoothreadermusic.ui.vm

import androidx.lifecycle.ViewModel
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

    private val _isReadingTestText = MutableStateFlow(false)
    val isReadingTestText: StateFlow<Boolean> = _isReadingTestText.asStateFlow()

    private val _permissionState = MutableStateFlow(GrantedPermissions())
    val permissionState: StateFlow<GrantedPermissions> = _permissionState.asStateFlow()

    init {
        onSetReadingTestText(false)
    }

    fun onSetInstalledApps(newInstalledApps: List<InstalledApp>) {
        _uiState.update { it.copy(addedApps = newInstalledApps) }
    }

    fun onChangeBtStatus(newBtStatus: String) {
        _uiState.update { it.copy(btStatus = newBtStatus)}
    }

    fun onSetVoicesCount(newVoicesCount: Int) {
        _uiState.update { it.copy(voicesCount = newVoicesCount) }
    }

    fun onSetReadNotificationsPermission(newReadNotificationsPermission: Boolean) {
        _permissionState.update { it.copy(readNotifications = newReadNotificationsPermission)}
    }

    fun onSetReadingTestText(newReadingTestText: Boolean) {
        _isReadingTestText.tryEmit(newReadingTestText)
    }

    fun onSetPostNotificationPermission(newPostNotificationPermission: Boolean) {
        _permissionState.update { it.copy(postNotification = newPostNotificationPermission) }
    }

    fun onSetBTStatusPermission(newBTStatusPermission: Boolean) {
        _permissionState.update { it.copy(btStatus = newBTStatusPermission) }
    }
}