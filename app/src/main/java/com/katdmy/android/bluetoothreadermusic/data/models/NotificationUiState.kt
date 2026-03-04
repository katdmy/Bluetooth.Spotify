package com.katdmy.android.bluetoothreadermusic.data.models

import android.content.Intent

data class NotificationUiState(
    val title: CharSequence,
    val icon: Int,
    val actionIntent: Intent,
    val label: String,
)
