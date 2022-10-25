package com.katdmy.android.bluetoothreadermusic.musicApps

import android.content.Intent
import android.graphics.drawable.Drawable

data class MusicApp(
    val name: String,
    val launchIntent: Intent?,
    val icon: Drawable
)