package com.katdmy.android.bluetoothreadermusic.ui.views

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PrivacyPolicyScreeen(
    url: String
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                loadUrl(url)
            }
        }
    )
}