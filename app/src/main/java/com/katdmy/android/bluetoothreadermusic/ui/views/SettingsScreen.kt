package com.katdmy.android.bluetoothreadermusic.ui.views

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.data.MessengerApp
import com.katdmy.android.bluetoothreadermusic.data.MusicApp
import com.katdmy.android.bluetoothreadermusic.ui.theme.BtReaderMusicTheme
import com.katdmy.android.bluetoothreadermusic.ui.views.helper.BtReaderButton
import com.katdmy.android.bluetoothreadermusic.ui.views.helper.MessengerAppColumn
import com.katdmy.android.bluetoothreadermusic.ui.views.helper.MusicAppRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    ttsModeSelection: Int?,
    installedMessengers: ArrayList<MessengerApp>,
    enabledMessengerString: String?,
    installedMusicApps: ArrayList<MusicApp>,
    selectedMusicApp: MusicApp,
    randomVoice: Boolean?,
    postNotificationPermissionGranted: Boolean,
    readNotificationsPermissionGranted: Boolean,
    btStatusPermissionGranted: Boolean,
    btStatus: String,
    onSetTtsMode: (Int) -> Unit,
    onCheckedChangeMessengerApp: (String, Boolean) -> Unit,
    onSelectMusicApp: (MusicApp) -> Unit,
    onSetRandomVoice: (Boolean) -> Unit,
    onClickStopService: () -> Unit,
    onClickStartService: () -> Unit,
    onClickServiceStatus: () -> Unit,
    onClickRequestReadNotificationsPermission: () -> Unit,
    onClickRequestPostNotificationPermission: () -> Unit,
    onClickRequestBtPermission: () -> Unit,
    onClickAbandonAudiofocus: () -> Unit,
    onClickPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val options = listOf(
        stringResource(R.string.mode_switch_allapps),
        stringResource(R.string.mode_switch_messengers)
    )

    // Используем фон, чтобы разделить карточки и создать ощущение глубины
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Секция переключения уведомлений по мессенджерам

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.mode_switch_header),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    options.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size
                            ),
                            onClick = { onSetTtsMode(index) },
                            selected = index == ttsModeSelection,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label, maxLines = 1)
                        }
                    }
                }

                if (installedMessengers.count() > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    MessengerAppColumn(
                        installedMessengerApps = installedMessengers,
                        enabledMessengerString = enabledMessengerString,
                        enabled = ttsModeSelection == 1,
                        onCheckedChangeMessengerApp = onCheckedChangeMessengerApp
                    )
                }
            }
        }

        if (installedMusicApps.count() > 0) {
            // Секция выбора музыкального приложения

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.music_app_header), style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    MusicAppRow(
                        installedMusicApps = installedMusicApps,
                        selectedMusicApp = selectedMusicApp,
                        onSelectMusicApp = onSelectMusicApp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Card(
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            // Секция включения случайного голоса
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.random_voice_header),
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Switch(
                    checked = randomVoice == true,
                    onCheckedChange = onSetRandomVoice
                )
            }
        }

        // Карточка для управления сервисом
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.service_header),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    BtReaderButton(
                        text = stringResource(R.string.service_stop),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        onClickAction = { onClickStopService() },
                        icon = ImageVector.vectorResource(R.drawable.ic_stop) // Добавляем иконку
                    )
                    BtReaderButton(
                        text = stringResource(R.string.service_info),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        onClickAction = {
                            onClickServiceStatus()
                            if (!postNotificationPermissionGranted)

                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.no_post_notification_permission),
                                        actionLabel = context.getString(R.string.enable_permission),
                                        duration = SnackbarDuration.Long
                                    )
                                    when (result) {
                                        SnackbarResult.ActionPerformed -> onClickRequestPostNotificationPermission()
                                        SnackbarResult.Dismissed -> {}
                                    }
                                }
                        },
                        icon = Icons.Default.Info // Добавляем иконку
                    )
                    BtReaderButton(
                        text = stringResource(R.string.service_start),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        onClickAction = {
                            onClickStartService()
                            if (!readNotificationsPermissionGranted)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.no_read_notifications_permission),
                                        actionLabel = context.getString(R.string.enable_permission),
                                        duration = SnackbarDuration.Long
                                    )
                                    when (result) {
                                        SnackbarResult.ActionPerformed -> onClickRequestReadNotificationsPermission()
                                        SnackbarResult.Dismissed -> {}
                                    }
                                }
                        },
                        icon = Icons.Default.PlayArrow // Добавляем иконку
                    )
                }

            }
        }

        // Секция для статуса Bluetooth
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.bt_status),
                    style = MaterialTheme.typography.headlineSmall
                ) // Заголовок крупнее
                Spacer(modifier = Modifier.weight(1f))
                if (btStatusPermissionGranted)
                    Text(
                        text = btStatus,
                        style = MaterialTheme.typography.bodyLarge
                    )
                else
                    BtReaderButton(
                        text = stringResource(R.string.enable_permission),
                        onClickAction = onClickRequestBtPermission
                    )
            }
        }

        // Секция кнопки сброса аудиофокуса

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            BtReaderButton(
                text = stringResource(R.string.abandon_audiofocus),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClickAction = onClickAbandonAudiofocus,
                icon = ImageVector.vectorResource(R.drawable.volume_up) // Добавляем иконку
            )
        }

        // Секция политики конфиденциальности

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            BtReaderButton(
                text = stringResource(R.string.privacy_policy_button),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClickAction = onClickPrivacyPolicy,
                icon = ImageVector.vectorResource(R.drawable.ic_privacy) // Добавляем иконку
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    BtReaderMusicTheme {
        SettingsScreen(
            scope = rememberCoroutineScope(),
            snackbarHostState = SnackbarHostState(),
            ttsModeSelection = 1,
            installedMessengers = arrayListOf(
                MessengerApp("org.whatsapp", "Whatsapp", null),
                MessengerApp("org.telegram.messenger", "Telegram", null),
            ),
            enabledMessengerString = null,
            installedMusicApps = arrayListOf(
                MusicApp("ru.yandex.music", null, "Яндекс Музыка", null),
                MusicApp("com.spotify.music", null, "Spotify", null),
                MusicApp("com.google.android.apps.youtube.music", null, "Youtube Music", null)
            ),
            randomVoice = false,
            postNotificationPermissionGranted = false,
            readNotificationsPermissionGranted = false,
            btStatusPermissionGranted = true,
            btStatus = "CONNECTED",
            selectedMusicApp = MusicApp("", null, "", null),
            onSetTtsMode = {},
            onCheckedChangeMessengerApp = { _, _ -> },
            onSelectMusicApp = {},
            onSetRandomVoice = {},
            onClickStopService = {},
            onClickStartService = {},
            onClickServiceStatus = {},
            onClickRequestReadNotificationsPermission = {},
            onClickRequestPostNotificationPermission = {},
            onClickRequestBtPermission = {},
            onClickAbandonAudiofocus = {},
            onClickPrivacyPolicy = {}
        )
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun SettingsScreenPreviewInRussian() {
    BtReaderMusicTheme {
        SettingsScreen(
            scope = rememberCoroutineScope(),
            snackbarHostState = SnackbarHostState(),
            ttsModeSelection = 1,
            installedMessengers = arrayListOf(
                MessengerApp("org.whatsapp", "Whatsapp", null),
                MessengerApp("org.telegram.messenger", "Telegram", null),
            ),
            enabledMessengerString = null,
            installedMusicApps = arrayListOf(
                MusicApp("ru.yandex.music", null, "Яндекс Музыка", null),
                MusicApp("com.spotify.music", null, "Spotify", null),
                MusicApp("com.google.android.apps.youtube.music", null, "Youtube Music", null)
            ),
            randomVoice = false,
            btStatusPermissionGranted = true,
            btStatus = "CONNECTED",
            selectedMusicApp = MusicApp("", null, "", null),
            onSetTtsMode = {},
            onCheckedChangeMessengerApp = { _, _ -> },
            onSelectMusicApp = {},
            postNotificationPermissionGranted = false,
            readNotificationsPermissionGranted = false,
            onSetRandomVoice = {},
            onClickStopService = {},
            onClickStartService = {},
            onClickServiceStatus = {},
            onClickRequestReadNotificationsPermission = {},
            onClickRequestPostNotificationPermission = {},
            onClickRequestBtPermission = {},
            onClickAbandonAudiofocus = {},
            onClickPrivacyPolicy = {}
        )
    }
}