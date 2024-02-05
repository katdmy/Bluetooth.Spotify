package com.katdmy.android.bluetoothreadermusic.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katdmy.android.bluetoothreadermusic.data.MainUiModel
import com.katdmy.android.bluetoothreadermusic.data.MusicApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ComposeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiModel())
    val uiState: StateFlow<MainUiModel> = _uiState.asStateFlow()

    fun onSetInstalledMusicApps(newInstalledMusicApps: ArrayList<MusicApp>) {
        viewModelScope.launch {
            _uiState.emit(_uiState.value.copy(installedMusicApps = newInstalledMusicApps))
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

    fun onChangeUseTTS(newUseTTS: Boolean) {
        viewModelScope.launch {
            _uiState.emit(_uiState.value.copy(useTTS = newUseTTS))
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
}