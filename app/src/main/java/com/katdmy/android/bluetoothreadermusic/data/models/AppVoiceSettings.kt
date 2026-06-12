package com.katdmy.android.bluetoothreadermusic.data.models

import com.katdmy.android.bluetoothreadermusic.data.enums.AudioFocusMode
import com.katdmy.android.bluetoothreadermusic.data.enums.NotificationPart
import kotlinx.serialization.Serializable

@Serializable
data class AppVoiceSettings(
    val packageName: String,
    val audioFocusMode: AudioFocusMode? = null,
    val enabledParts: Set<NotificationPart>? = null
)
