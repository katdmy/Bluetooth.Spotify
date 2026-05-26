package com.katdmy.android.bluetoothreadermusic.ui.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.ui.theme.BtReaderMusicTheme
import com.katdmy.android.bluetoothreadermusic.ui.views.helper.BtReaderButton

@Composable
fun MainScreen(
    testTextToSpeech: String,
    isReadingTestText: Boolean,
    showLog: Boolean,
    messages: List<String>,
    useTTS: Boolean,
    onTestTextToSpeechChange: (String) -> Unit,
    onClickReadTestText: (String) -> Unit,
    onClickStopReading: () -> Unit,
    onClearLog: () -> Unit,
    onChangeUseTTS: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        val alpha =
            if (isSystemInDarkTheme()) 0.08f
            else 0.04f
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                MaterialTheme.colorScheme.onBackground
            ),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(280.dp)
                .alpha(alpha)
                .align(Alignment.Center)
        )

        Column(
            modifier = Modifier.padding(12.dp),
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
                                painter = painterResource(R.drawable.ic_clear),
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
                            painter = painterResource(R.drawable.ic_close)
                        )
                    else
                    // Кнопка чтения текста
                        BtReaderButton(
                            text = stringResource(R.string.tts_read),
                            onClickAction = { onClickReadTestText(testTextToSpeech) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            painter = painterResource(R.drawable.ic_music_note)
                        )
                }
            }

            if (showLog)
                Card(
                    modifier = modifier.weight(1f),
                    elevation = CardDefaults.elevatedCardElevation(4.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.log_messages),
                            style = MaterialTheme.typography.headlineMedium, // Красивый заголовок
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        LazyColumn(modifier = Modifier
                            .fillMaxWidth().weight(1f)
                        ) {
                            items(messages) { message ->
                                Text(text = message)
                            }
                        }
                        BtReaderButton(
                            text = stringResource(R.string.log_messages_clear),
                            onClickAction = onClearLog,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            painter = painterResource(R.drawable.ic_delete)
                        )
                    }
                }
            else
                Spacer(modifier = Modifier.weight(1f))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(4.dp),
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
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    BtReaderMusicTheme {
        MainScreen(
            testTextToSpeech = "",
            isReadingTestText = false,
            showLog = true,
            messages = listOf(
                "[12:41:02] BootReceiver triggered",
                "[12:41:15] Worker started",
                "[12:41:22] Bluetooth disconnected",
                "[12:41:33] Bluetooth connected",
                "[12:41:35] TTS initialized",
                "[12:41:40] Voice selected: ko-KR-voice-3"
            ),
            onTestTextToSpeechChange = {},
            onClickReadTestText = {},
            onClickStopReading = {},
            onClearLog = {},
            onChangeUseTTS = {},
            useTTS = false,
        )
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun MainScreenPreviewInRussian() {
    BtReaderMusicTheme {
        MainScreen(
            testTextToSpeech = "",
            isReadingTestText = false,
            showLog = false,
            messages = listOf(
                "[12:41:02] BootReceiver triggered",
                "[12:41:15] Worker started",
                "[12:41:22] Bluetooth disconnected",
                "[12:41:33] Bluetooth connected",
                "[12:41:35] TTS initialized",
                "[12:41:40] Voice selected: ko-KR-voice-3"
            ),
            onTestTextToSpeechChange = {},
            onClickReadTestText = {},
            onClickStopReading = {},
            onClearLog = {},
            onChangeUseTTS = {},
            useTTS = false,
        )
    }
}