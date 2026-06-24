package com.katdmy.android.bluetoothreadermusic.data.models

import com.katdmy.android.bluetoothreadermusic.data.enums.AudioFocusMode
import com.katdmy.android.bluetoothreadermusic.data.enums.NotificationPart


data class MainUiModel(
    val installedApps: List<InstalledApp> = emptyList(),
    val addedApps: List<InstalledApp> = emptyList(),
    val isReadingTestText: Boolean = false,
    val voicesCount: Int = 0,
    val globalAudioFocusMode: AudioFocusMode = AudioFocusMode.DUCK,
    val globalNotificationParts: Set<NotificationPart> = setOf(
        NotificationPart.TITLE,
        NotificationPart.TEXT
    ),
    val allAppSettings: List<AppVoiceSettings> = emptyList(),
    val readUpdates: Boolean = true,
    val useTTS: Boolean = false,
    val ttsModeSelection: Int = 0,
    val randomVoice: Boolean = false,
    val ttsVolume: Float = 1f,
    val showLog: Boolean = false,
)