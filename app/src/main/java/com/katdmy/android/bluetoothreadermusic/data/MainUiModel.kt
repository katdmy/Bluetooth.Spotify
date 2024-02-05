package com.katdmy.android.bluetoothreadermusic.data


data class MainUiModel(
    var btStatus: String = "",
    var logMessages: String = "",
    var installedMusicApps: ArrayList<MusicApp> = arrayListOf(),
    var selectedMusicApp: MusicApp = MusicApp(
        packageName = "",
        launchIntent = null,
        name = "",
        icon = null
    ),
    var useTTS: Boolean = false
)