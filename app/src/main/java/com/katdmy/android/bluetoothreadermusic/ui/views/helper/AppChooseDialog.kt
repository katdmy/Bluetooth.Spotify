package com.katdmy.android.bluetoothreadermusic.ui.views.helper

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.data.models.InstalledApp
import com.katdmy.android.bluetoothreadermusic.ui.theme.BtReaderMusicTheme
import kotlinx.coroutines.delay

@Composable
fun AppChooseDialog(
    installedApps: List<InstalledApp>,
    alreadyAdded: List<String>,
    onClickAdd: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = remember { mutableStateMapOf<String, Boolean>() }
    var appSearchString by remember { mutableStateOf("") }
    var debouncedSearch by remember { mutableStateOf("") }

    LaunchedEffect(installedApps) {
        installedApps.forEach {
            selected[it.packageName] = false
        }
    }

    LaunchedEffect(appSearchString) {
        delay(300)
        debouncedSearch = appSearchString
    }

    val filteredApps = remember(debouncedSearch, installedApps) {
        if (debouncedSearch.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.name.contains(appSearchString, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.9f)
                    .imePadding()
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = stringResource(R.string.app_choose_dialog_header),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )

                    HorizontalDivider()

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val isAlreadyAdded = alreadyAdded.contains(app.packageName)
                            val isChecked = selected[app.packageName] ?: false

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAlreadyAdded) {
                                        selected[app.packageName] = !isChecked
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                app.icon?.let {
                                    Image(
                                        bitmap = it.toBitmap().asImageBitmap(),
                                        contentDescription = "App Icon",
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = app.name,
                                    modifier = Modifier.weight(1f)
                                )

                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (!isAlreadyAdded) {
                                            selected[app.packageName] = it
                                        }
                                    },
                                    enabled = !isAlreadyAdded
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = appSearchString,
                        onValueChange = { appSearchString = it },
                        label = { Text(text = stringResource(R.string.app_choose_dialog_search_hint)) },
                        trailingIcon = {
                            Image(
                                painter = painterResource(R.drawable.ic_clear),
                                contentDescription = null,
                                modifier = Modifier.clickable { appSearchString = "" }
                            )
                        },
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.app_choose_dialog_cancel_btn))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val result = selected.filter {
                                    it.value && !alreadyAdded.contains(it.key)
                                }.keys.toList()

                                onClickAdd(result)
                                onDismiss()
                            }
                        ) {
                            Text(stringResource(R.string.app_choose_dialog_add_btn))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppChooseDialogPreview() {
    BtReaderMusicTheme {
        AppChooseDialog(
            installedApps = listOf(
                InstalledApp("org.telegram.messenger", "Telegram", null),
                InstalledApp("org.whatsapp", "Whatsapp", null),
            ),
            alreadyAdded = listOf(
                "org.whatsapp",
            ),
            onClickAdd = {},
            onDismiss = {}
        )
    }
}