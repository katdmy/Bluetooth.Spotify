package com.katdmy.android.bluetoothreadermusic.ui.views.helper

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.data.models.InstalledApp
import com.katdmy.android.bluetoothreadermusic.ui.theme.BtReaderMusicTheme
import kotlin.collections.forEach

@Composable
fun AddedAppColumn(
    addedApps: List<InstalledApp>,
    enabled: Boolean,
    onClickOpenAppSettings: (String) -> Unit,
    onClickDeleteApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column( // Вертикальный скролл для приложений
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        addedApps.forEach { messengerApp ->
            MessengerAppCard(
                messengerApp = messengerApp,
                enabled = enabled,
                onClickOpenAppSettings = { onClickOpenAppSettings(messengerApp.packageName) },
                onClickDelete = { onClickDeleteApp(messengerApp.packageName) },
            )
        }
    }
}

@Composable
fun MessengerAppCard(
    messengerApp: InstalledApp,
    enabled: Boolean,
    onClickOpenAppSettings: () -> Unit,
    onClickDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(4.dp),
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = rememberDrawablePainter(drawable = messengerApp.icon),
                contentDescription = "${messengerApp.name} icon",
                modifier = Modifier
                    .alpha(if (!enabled) 0.5f else 1f)
                    .size(48.dp)
                    .padding(6.dp)
            )
            Text(
                text = messengerApp.name ?: "Unknown app",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (!enabled) 0.5f else 1f)
                    .padding(start = 4.dp)
            )
            IconButton(
                onClick = onClickOpenAppSettings,
                enabled = enabled,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = "App settings"
                )
            }
            IconButton(
                onClick = onClickDelete,
                enabled = enabled,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "Delete"
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddedAppColumnPreview() {
    BtReaderMusicTheme {
        AddedAppColumn(
            addedApps = listOf(
                InstalledApp("org.whatsapp", "Whatsapp", null),
                InstalledApp("org.telegram.messenger", "Telegram", null),
            ),
            enabled = true,
            onClickOpenAppSettings = {},
            onClickDeleteApp = {},
        )
    }
}