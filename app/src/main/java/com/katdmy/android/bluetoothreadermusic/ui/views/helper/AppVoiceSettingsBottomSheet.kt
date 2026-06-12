package com.katdmy.android.bluetoothreadermusic.ui.views.helper

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.katdmy.android.bluetoothreadermusic.data.enums.AudioFocusMode
import com.katdmy.android.bluetoothreadermusic.data.enums.NotificationPart
import com.katdmy.android.bluetoothreadermusic.data.models.AppVoiceSettings
import com.katdmy.android.bluetoothreadermusic.ui.theme.BtReaderMusicTheme
import com.katdmy.android.bluetoothreadermusic.util.PackageHelper.getAppName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppVoiceSettingsBottomSheet(
    settings: AppVoiceSettings,
    sheetState: SheetState,
    onClickSaveAppSettings: (AppVoiceSettings) -> Unit,
    onCloseAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var useGlobalAudioFocusMode by remember { mutableStateOf(settings.audioFocusMode == null) }
    var audioFocusMode by remember { mutableStateOf(settings.audioFocusMode) }
    var useGlobalNotificationParts by remember { mutableStateOf(settings.enabledParts == null) }
    var enabledParts by remember { mutableStateOf(settings.enabledParts) }
    val appName = getAppName(context, settings.packageName) ?: "This app"

    ModalBottomSheet(
        onDismissRequest = onCloseAppSettings,
        sheetState = sheetState
    ) {
        Column(modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(vertical = 16.dp)
        ) {
            Text(
                text = appName,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            AudioFocusModeSelector(
                isGlobal = false,
                useGlobal = useGlobalAudioFocusMode,
                selected = audioFocusMode,
                onChangeUseGlobal = { useGlobalAudioFocusMode = it },
                onSelect = { audioFocusMode = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            NotificationPartsSelector(
                isGlobal = false,
                useGlobal = useGlobalNotificationParts,
                selectedParts = enabledParts,
                appName = appName,
                onChangeUseGlobal = { useGlobalNotificationParts = it },
                onChange = { enabledParts = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    onClickSaveAppSettings(
                        settings.copy(
                            enabledParts = if (useGlobalNotificationParts) null else enabledParts,
                            audioFocusMode = if (useGlobalAudioFocusMode) null else audioFocusMode
                        )
                    )
                    onCloseAppSettings()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun AppVoiceSettingsBottomSheetPreview() {
    BtReaderMusicTheme {
        AppVoiceSettingsBottomSheet(
            settings = AppVoiceSettings(
                packageName = "org.telegram.com",
                enabledParts = setOf(NotificationPart.TITLE, NotificationPart.TEXT),
                audioFocusMode = AudioFocusMode.DUCK
            ),
            sheetState = rememberModalBottomSheetState(),
            onClickSaveAppSettings = {},
            onCloseAppSettings = {}
        )
    }
}