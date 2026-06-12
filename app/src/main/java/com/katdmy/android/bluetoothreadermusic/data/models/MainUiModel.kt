package com.katdmy.android.bluetoothreadermusic.data.models

import com.katdmy.android.bluetoothreadermusic.data.enums.AudioFocusMode
import com.katdmy.android.bluetoothreadermusic.data.enums.NotificationPart


data class MainUiModel(
    val installedApps: List<InstalledApp> = emptyList(),
    val addedApps: List<InstalledApp> = emptyList(),
    val testTextToSpeech: String = "",
    val voicesCount: Int = 0,
    val globalAudioFocusMode: AudioFocusMode = AudioFocusMode.DUCK,
    val globalNotificationParts: Set<NotificationPart> = setOf(
        NotificationPart.TITLE,
        NotificationPart.TEXT
    ),
    val allAppSettings: List<AppVoiceSettings> = emptyList()
)