package com.katdmy.android.bluetoothreadermusic.ui.views.helper

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.data.enums.AudioFocusMode
import com.katdmy.android.bluetoothreadermusic.ui.theme.BtReaderMusicTheme
import kotlin.collections.forEach

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioFocusModeSelector(
    isGlobal: Boolean,
    useGlobal: Boolean,
    selected: AudioFocusMode?,
    onChangeUseGlobal: (Boolean) -> Unit,
    onSelect: (AudioFocusMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.notification_audiofocus_header),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!isGlobal) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = useGlobal,
                    onCheckedChange = onChangeUseGlobal,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.notification_settings_use_global),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(
            modifier = modifier.fillMaxWidth(0.9f)
        ) {
            OutlinedTextField(
                value = getName(selected, context),
                onValueChange = {},
                readOnly = true,
                label = { Text(text = stringResource(R.string.notification_audiofocus_header)) },
                trailingIcon = {
                    TrailingIcon(
                        expanded = expanded,
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (useGlobal) 0.7f else 1f),
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                AudioFocusMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(getName(mode, context)) },
                        onClick = {
                            onSelect(mode)
                            expanded = false
                        }
                    )
                }
            }
        }
        if (isGlobal || !useGlobal) {
            Text(
                text = getDescription(selected, context),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getName(mode: AudioFocusMode?, context: Context): String =
    when (mode) {
        AudioFocusMode.DUCK -> context.getString(R.string.notification_audiofocus_duck)
        AudioFocusMode.EXCLUSIVE -> context.getString(R.string.notification_audiofocus_exclusive)
        else -> context.getString(R.string.notification_settings_default)
    }

private fun getDescription(mode: AudioFocusMode?, context: Context): String =
    when (mode) {
        AudioFocusMode.DUCK ->
            context.getString(R.string.notification_audiofocus_duck_description)
        AudioFocusMode.EXCLUSIVE ->
            context.getString(R.string.notification_audiofocus_exclusive_description)
        null ->
            context.getString(R.string.notification_settings_use_global)
    }


@Preview(showBackground = true)
@Composable
fun CategorySelectorPreview() {
    BtReaderMusicTheme {
        AudioFocusModeSelector(
            isGlobal = false,
            useGlobal = false,
            selected = AudioFocusMode.DUCK,
            onChangeUseGlobal = {},
            onSelect = {}
        )
    }
}