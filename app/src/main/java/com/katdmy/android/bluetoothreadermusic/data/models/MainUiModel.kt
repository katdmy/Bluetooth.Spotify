package com.katdmy.android.bluetoothreadermusic.data.models


data class MainUiModel(
    val btStatus: String = "",
    val logMessages: String = "",
    val addedApps: List<InstalledApp> = listOf(),
    val testTextToSpeech: String = "",
    val voicesCount: Int = 0,
)