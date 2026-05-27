package com.katdmy.android.bluetoothreadermusic.data.models


data class MainUiModel(
    val installedApps: List<InstalledApp> = listOf(),
    val addedApps: List<InstalledApp> = listOf(),
    val testTextToSpeech: String = "",
    val voicesCount: Int = 0,
)