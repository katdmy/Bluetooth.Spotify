package com.katdmy.android.bluetoothreadermusic.ui.views.helper

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.data.ServiceStatus
import com.katdmy.android.bluetoothreadermusic.services.StatusService
import com.katdmy.android.bluetoothreadermusic.ui.theme.BtReaderMusicTheme

@Composable
fun ServiceHealthIndicator(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val health = StatusService.serviceHealth.collectAsState()

    Icon(
        painter = painterResource(R.drawable.ic_circle),
        contentDescription = "Indicator",
        tint = when (health.value) {
            ServiceStatus.Working -> Color.Green
            ServiceStatus.Dead -> Color.Red
            else -> Color.Gray
        },
        modifier = modifier.clickable(onClick = onClick)
    )
}

@Preview(showBackground = true)
@Composable
fun ServiceHealthIndicatorWorking() {
    BtReaderMusicTheme {
        ServiceHealthIndicator({})
    }
}