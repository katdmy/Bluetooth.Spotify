package com.katdmy.android.bluetoothreadermusic.ui.views.helper

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.data.enums.NotificationPart
import com.katdmy.android.bluetoothreadermusic.ui.theme.BtReaderMusicTheme

@Composable
fun NotificationPartsSelector(
    isGlobal: Boolean,
    useGlobal: Boolean,
    selectedParts: Set<NotificationPart>?,
    appName: String,
    onChangeUseGlobal: (Boolean) -> Unit,
    onChange: (Set<NotificationPart>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var partsPreview by remember {
        mutableStateOf(getPreview(selectedParts, appName, context))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.notification_parts_header),
            style = MaterialTheme.typography.titleMedium
        )

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
        }

        FlowRow(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            NotificationPart.entries.forEach { part ->
                val label: String = when (part) {
                    NotificationPart.APP -> stringResource(R.string.notification_parts_app)
                    NotificationPart.TITLE -> stringResource(R.string.notification_parts_title)
                    NotificationPart.TEXT -> stringResource(R.string.notification_parts_text)
                }

                FilterChip(
                    selected = selectedParts?.contains(part) == true,
                    onClick = {
                        val updated =
                            if (selectedParts?.contains(part) ?: false)
                                selectedParts - part
                            else
                                if (selectedParts == null)
                                    setOf(part)
                                else
                                    selectedParts + part

                        onChange(updated)
                        partsPreview = getPreview(updated, appName, context)
                    },
                    enabled = isGlobal || !useGlobal,
                    label = { Text(label) },
                )
            }
        }
        if (isGlobal || !useGlobal) {
            Text(
                text = stringResource(R.string.notification_parts_preview_header),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = partsPreview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getPreview(
    selectedParts: Set<NotificationPart>?,
    appName: String,
    context: Context
): String {
    val preview = StringBuilder()
    if (selectedParts?.contains(NotificationPart.APP) == true)
        preview.append("$appName. ")
    if (selectedParts?.contains(NotificationPart.TITLE) == true)
        preview.append("${context.getString(R.string.notification_parts_preview_title)} ")
    if (selectedParts?.contains(NotificationPart.TEXT) == true)
        preview.append(context.getString(R.string.notification_parts_preview_text))
    return preview.toString()
}

@Preview(showBackground = true)
@Composable
fun NotificationPartsSelectorPreview() {
    BtReaderMusicTheme {
        NotificationPartsSelector(
            isGlobal = false,
            useGlobal = false,
            selectedParts = setOf(NotificationPart.TITLE, NotificationPart.TEXT),
            appName = "Messenger",
            onChangeUseGlobal = {},
            onChange = {}
        )
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun NotificationPartsSelectorPreviewRu() {
    BtReaderMusicTheme {
        NotificationPartsSelector(
            isGlobal = false,
            useGlobal = false,
            selectedParts = setOf(NotificationPart.TITLE, NotificationPart.TEXT),
            appName = "Messenger",
            onChangeUseGlobal = {},
            onChange = {}
        )
    }
}