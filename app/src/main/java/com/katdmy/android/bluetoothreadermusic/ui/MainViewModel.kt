package com.katdmy.android.bluetoothreadermusic.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katdmy.android.bluetoothreadermusic.data.MainUiModel
import com.katdmy.android.bluetoothreadermusic.data.MessengerApp
import com.katdmy.android.bluetoothreadermusic.data.MusicApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiModel())
    val uiState: StateFlow<MainUiModel> = _uiState.asStateFlow()

    fun onSetInstalledMusicApps(newInstalledMusicApps: ArrayList<MusicApp>) {
        viewModelScope.launch {
            _uiState.emit(_uiState.value.copy(installedMusicApps = newInstalledMusicApps))
        }
    }

    fun onSetInstalledMessengerApps(newInstalledMessengerApps: ArrayList<MessengerApp>) {
        viewModelScope.launch {
            _uiState.emit(_uiState.value.copy(installedMessengerApps = newInstalledMessengerApps))
        }
    }

    fun onAddLogMessage(log: String) {
        viewModelScope.launch {
            val newLog = uiState.value.logMessages + "\n" + log
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

    fun onSelectMusicApp(newMusicApp: MusicApp) {
        viewModelScope.launch {
            _uiState.emit(_uiState.value.copy(selectedMusicApp = newMusicApp))
        }
    }

    fun onSelectMusicAppByPackageName(newMusicAppPackageName: String?) {
        viewModelScope.launch {
            val newMusicApp = _uiState.value.installedMusicApps.find { it.packageName == newMusicAppPackageName }
            if (newMusicApp != null)
              _uiState.emit(_uiState.value.copy(selectedMusicApp = newMusicApp))
        }
    }

    fun toggleSettingsShown() {
        viewModelScope.launch {
            _uiState.emit(_uiState.value.copy(settingsShown = !_uiState.value.settingsShown))
        }
    }
}