package com.katdmy.android.bluetoothreadermusic.data


data class MainUiModel(
    val btStatus: String = "",
    val logMessages: String = "",
    val installedMusicApps: ArrayList<MusicApp> = arrayListOf(),
    val selectedMusicApp: MusicApp = MusicApp(
        packageName = "",
        launchIntent = null,
        name = "",
        icon = null
    ),
    val installedMessengerApps: ArrayList<MessengerApp> = arrayListOf(),
    val testTextToSpeech: String = ""
)