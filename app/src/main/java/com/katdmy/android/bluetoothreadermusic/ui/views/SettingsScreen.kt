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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.data.ServiceStatus
import com.katdmy.android.bluetoothreadermusic.data.models.InstalledApp
import com.katdmy.android.bluetoothreadermusic.services.StatusService
import com.katdmy.android.bluetoothreadermusic.ui.theme.BtReaderMusicTheme
import com.katdmy.android.bluetoothreadermusic.ui.views.helper.AppChooseDialog
import com.katdmy.android.bluetoothreadermusic.ui.views.helper.BtReaderButton
import com.katdmy.android.bluetoothreadermusic.ui.views.helper.InstalledAppColumn
import com.katdmy.android.bluetoothreadermusic.ui.views.helper.ServiceHealthIndicator

@Composable
fun SettingsScreen(
    ttsModeSelection: Int?,
    addedApps: List<InstalledApp>,
    randomVoice: Boolean?,
    voicesCount: Int,
    postNotificationPermissionGranted: Boolean,
    readNotificationsPermissionGranted: Boolean,
    btStatusPermissionGranted: Boolean,
    btStatus: String,
    onGetInstalledLaunchableApps: () -> List<InstalledApp>,
    onSetTtsMode: (Int) -> Unit,
    onClickDeleteApp: (String) -> Unit,
    onClickAddApp: (List<String>) -> Unit,
    onSetRandomVoice: (Boolean) -> Unit,
    openNotificationSettings: () -> Unit,
    onClickRequestReadNotificationsPermission: () -> Unit,
    onClickRequestPostNotificationPermission: () -> Unit,
    onClickRequestBtPermission: () -> Unit,
    onClickOpenTTSSettings: () -> Unit,
    onClickForceRestartTTS: () -> Unit,
    onClickPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val serviceHealth by StatusService.serviceHealth.collectAsState()
    val options = listOf(
        stringResource(R.string.mode_switch_allapps),
        stringResource(R.string.mode_switch_selected)
    )
    var openAppChooseDialog by remember { mutableStateOf(false) }

    // Используем фон, чтобы разделить карточки и создать ощущение глубины
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Карточка состояния сервиса
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.service_header),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    ServiceHealthIndicator(
                        onClick = onClickRequestReadNotificationsPermission
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (!readNotificationsPermissionGranted) {
                    Text(
                        text = stringResource(R.string.no_read_notifications_permission),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.allow_read_notifications_permission),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    BtReaderButton(
                        text = stringResource(R.string.open_settings),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        onClickAction = openNotificationSettings,
                        painter = painterResource(R.drawable.ic_settings)
                    )
                } else if (!postNotificationPermissionGranted) {
                    Text(
                        text = stringResource(R.string.no_post_notification_permission),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    BtReaderButton(
                        text = stringResource(R.string.open_settings),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        onClickAction = onClickRequestPostNotificationPermission,
                        painter = painterResource(R.drawable.ic_notifications)
                    )
                } else {
                    when (serviceHealth) {
                        ServiceStatus.Dead -> {
                            Text(
                                text = stringResource(R.string.service_not_responding),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Text(
                                text = stringResource(R.string.restart_read_notifications_permission),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            BtReaderButton(
                                text = stringResource(R.string.open_settings),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                onClickAction = openNotificationSettings,
                                painter = painterResource(R.drawable.ic_settings)
                            )
                        }
                        ServiceStatus.Disabled -> {
                            Text(
                                text = stringResource(R.string.service_stopped),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                        ServiceStatus.Working -> {
                            Text(
                                text = stringResource(R.string.service_working),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Секция уведомлений по приложениям
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

                if (addedApps.count() > 0) {
                    InstalledAppColumn(
                        installedApps = addedApps,
                        enabled = ttsModeSelection == 1,
                        onClickDeleteApp = onClickDeleteApp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BtReaderButton(
                        text = stringResource(R.string.add_installed_app),
                        onClickAction = { openAppChooseDialog = true },
                        painter = painterResource(R.drawable.ic_add),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            }
        }

        Card(
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            // Секция включения случайного голоса
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                if (voicesCount <= 1) {
                    Text(
                        text = stringResource(R.string.lack_available_voices),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
                BtReaderButton(
                    text = stringResource(R.string.open_tts_settings),
                    onClickAction = onClickOpenTTSSettings,
                    painter = painterResource(R.drawable.ic_settings),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
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
                        onClickAction = onClickRequestBtPermission,
                        modifier = Modifier,
                        painter = null,
                        enabled = true
                    )
            }
        }

        // Секция кнопки перезапуска TTS

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            BtReaderButton(
                text = stringResource(R.string.restart_tts),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClickAction = onClickForceRestartTTS,
                painter = painterResource(R.drawable.volume_up) // Добавляем иконку
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
                painter = painterResource(R.drawable.ic_privacy) // Добавляем иконку
            )
        }
    }

    if (openAppChooseDialog) {
        val installedLaunchableApps = onGetInstalledLaunchableApps()
        AppChooseDialog(
            installedApps = installedLaunchableApps,
            alreadyAdded = addedApps.map { it.packageName },
            onClickAdd = onClickAddApp,
            onDismiss = { openAppChooseDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    BtReaderMusicTheme {
        SettingsScreen(
            ttsModeSelection = 1,
            addedApps = listOf(
                InstalledApp("org.whatsapp", "Whatsapp", null),
                InstalledApp("org.telegram.messenger", "Telegram", null),
            ),
            randomVoice = false,
            voicesCount = 1,
            postNotificationPermissionGranted = false,
            readNotificationsPermissionGranted = false,
            btStatusPermissionGranted = true,
            btStatus = "CONNECTED",
            onGetInstalledLaunchableApps = { emptyList() },
            onSetTtsMode = {},
            onClickDeleteApp = {},
            onClickAddApp = {},
            onSetRandomVoice = {},
            onClickOpenTTSSettings = {},
            openNotificationSettings = {},
            onClickRequestReadNotificationsPermission = {},
            onClickRequestPostNotificationPermission = {},
            onClickRequestBtPermission = {},
            onClickForceRestartTTS = {},
            onClickPrivacyPolicy = {}
        )
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun SettingsScreenPreviewInRussian() {
    BtReaderMusicTheme {
        SettingsScreen(
            ttsModeSelection = 1,
            addedApps = listOf(
                InstalledApp("org.whatsapp", "Whatsapp", null),
                InstalledApp("org.telegram.messenger", "Telegram", null),
            ),
            randomVoice = false,
            voicesCount = 2,
            btStatusPermissionGranted = true,
            btStatus = "CONNECTED",
            onGetInstalledLaunchableApps = { emptyList() },
            onSetTtsMode = {},
            onClickDeleteApp = {},
            onClickAddApp = {},
            postNotificationPermissionGranted = false,
            readNotificationsPermissionGranted = false,
            onSetRandomVoice = {},
            onClickOpenTTSSettings = {},
            openNotificationSettings = {},
            onClickRequestReadNotificationsPermission = {},
            onClickRequestPostNotificationPermission = {},
            onClickRequestBtPermission = {},
            onClickForceRestartTTS = {},
            onClickPrivacyPolicy = {}
        )
    }
}