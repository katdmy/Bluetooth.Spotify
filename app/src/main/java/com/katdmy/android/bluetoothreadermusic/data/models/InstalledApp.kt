package com.katdmy.android.bluetoothreadermusic.data.models

import android.graphics.drawable.Drawable

data class InstalledApp(
    val packageName: String,
    val name: String,
    val icon: Drawable?
)