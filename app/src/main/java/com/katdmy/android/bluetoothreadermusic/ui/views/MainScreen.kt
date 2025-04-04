package com.katdmy.android.bluetoothreadermusic.ui.views

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.data.models.MusicApp
import com.katdmy.android.bluetoothreadermusic.ui.theme.BtReaderMusicTheme
import com.katdmy.android.bluetoothreadermusic.ui.views.helper.BtReaderButton

@Composable
fun MainScreen(
    testTextToSpeech: String,
    onTestTextToSpeechChange: (String) -> Unit,
    logMessages: String,
    useTTS: Boolean,
    selectedMusicApp: MusicApp,
    isReadingTestText: Boolean,
    onClickReadTestText: (String) -> Unit,
    onClickStopReading: () -> Unit,
    onClearLog: () -> Unit,
    onChangeUseTTS: (Boolean) -> Unit,
    onClickOpenMusic: (launchMusicAppIntent: Intent?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Используем фон, чтобы разделить карточки и создать ощущение глубины
    Column(modifier = modifier
        .background(MaterialTheme.colorScheme.background)
        .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Секция для проверки работы TTS
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = testTextToSpeech,
                    onValueChange = { onTestTextToSpeechChange(it) },
                    label = {
                        Text(text = stringResource(R.string.tts_test_label))
                    },
                    trailingIcon = {
                        Image(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.clickable { onTestTextToSpeechChange("") }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isReadingTestText)
                // Кнопка остановки чтения текста
                    BtReaderButton(
                        text = stringResource(R.string.tts_stop),
                        onClickAction = { onClickStopReading() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        icon = ImageVector.vectorResource(R.drawable.ic_close)
                    )
                else
                // Кнопка чтения текста
                    BtReaderButton(
                        text = stringResource(R.string.tts_read),
                        onClickAction = { onClickReadTestText(testTextToSpeech) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        icon = ImageVector.vectorResource(R.drawable.ic_music_note)
                    )
            }
        }

        // Секция для логов
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.log_messages),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = logMessages,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                )
                // Кнопка очистки логов
                BtReaderButton(
                    text = stringResource(R.string.log_messages_clear),
                    onClickAction = onClearLog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    icon = Icons.Default.Delete // Иконка удаления
                )
            }
        }

        Card(
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            // Переключатель TTS
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(vertical = 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.text_to_speech),
                    style = MaterialTheme.typography.headlineMedium, // Красивый заголовок
                    modifier = Modifier.padding(end = 12.dp)
                )
                Switch(
                    checked = useTTS,
                    onCheckedChange = onChangeUseTTS
                )
            }
        }

        // Кнопка открытия приложения
        Card(
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.music_header),
                    style = MaterialTheme.typography.headlineSmall, // Красивый заголовок
                    modifier = Modifier.padding(end = 12.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                BtReaderButton(
                    text = stringResource(R.string.music_open, selectedMusicApp.name),
                    onClickAction = { onClickOpenMusic(selectedMusicApp.launchIntent) },
                    modifier = Modifier.weight(2f),
                    enabled = selectedMusicApp.launchIntent != null,
                    icon = ImageVector.vectorResource(R.drawable.ic_music_note)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    BtReaderMusicTheme {
        MainScreen(
            onClickReadTestText = {},
            onClickStopReading = {},
            onChangeUseTTS = {},
            onClickOpenMusic = {},
            testTextToSpeech = "",
            onTestTextToSpeechChange = {},
            logMessages = "",
            useTTS = false,
            selectedMusicApp = MusicApp("com.spotify.org", null, "Spotify", null),
            isReadingTestText = false,
            onClearLog = {}
        )
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun MainScreenPreviewInRussian() {
    BtReaderMusicTheme {
        MainScreen(
            onClickReadTestText = {},
            onClickStopReading = {},
            onChangeUseTTS = {},
            onClickOpenMusic = {},
            testTextToSpeech = "",
            onTestTextToSpeechChange = {},
            logMessages = "",
            useTTS = false,
            selectedMusicApp = MusicApp("com.spotify.org", null, "Spotify", null),
            isReadingTestText = false,
            onClearLog = {}
        )
    }
}