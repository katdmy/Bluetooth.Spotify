package com.katdmy.android.bluetoothreadermusic.ui.views.helper

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.data.models.MessengerApp
import com.katdmy.android.bluetoothreadermusic.util.StringListHelper.getList
import kotlin.collections.forEach

@Composable
fun MessengerAppColumn(
    installedMessengerApps: ArrayList<MessengerApp>,
    enabledMessengerString: String?,
    enabled: Boolean,
    onCheckedChangeMessengerApp: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.messengers_header),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.alpha(if (!enabled) 0.5f else 1f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column( // Вертикальный скролл для приложений
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            installedMessengerApps.forEach { messengerApp ->
                MessengerAppCard(
                    messengerApp = messengerApp,
                    enabledMessengerString = enabledMessengerString,
                    enabled = enabled,
                    onCheckedChangeMessengerApp = onCheckedChangeMessengerApp
                )
            }
        }
    }
}

@Composable
fun MessengerAppCard(
    messengerApp: MessengerApp,
    enabledMessengerString: String?,
    enabled: Boolean,
    onCheckedChangeMessengerApp: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(4.dp),
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
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
                text = messengerApp.name,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(if (!enabled) 0.5f else 1f)
                    .padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = enabledMessengerString?.getList()?.contains(messengerApp.packageName) == true,
                onCheckedChange = { checked: Boolean ->
                    onCheckedChangeMessengerApp(messengerApp.packageName, checked)
                },
                enabled = enabled,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    }
}