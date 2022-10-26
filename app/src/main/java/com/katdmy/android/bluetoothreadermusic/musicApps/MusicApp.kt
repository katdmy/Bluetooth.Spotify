package com.katdmy.android.bluetoothreadermusic.musicApps

import android.content.Intent
import android.graphics.drawable.Drawable

data class MusicApp(
    val packageName: String,
    val launchIntent: Intent?,
    val name: String,
    val icon: Drawable
)