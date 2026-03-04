package com.katdmy.android.bluetoothreadermusic.ui.views.helper

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.ui.theme.BtReaderMusicTheme

@Composable
fun BtReaderButton(
    text: String,
    onClickAction: () -> Unit,
    modifier: Modifier = Modifier,
    painter: Painter? = null,
    enabled: Boolean = true
) {
    ElevatedButton(
        modifier = modifier,
        onClick = onClickAction,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors().copy(containerColor = MaterialTheme.colorScheme.primaryContainer),
        contentPadding = PaddingValues()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)) {
            painter?.let {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.size(4.dp))
            }
            Text(
                text = text,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BTReaderButtonPreviewEn() {
    BtReaderMusicTheme {
        BtReaderButton(
            text = stringResource(R.string.add_installed_app),
            onClickAction = {},
            painter = painterResource(R.drawable.ic_add)
        )
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun BTReaderButtonPreviewRu() {
    BtReaderMusicTheme {
        BtReaderButton(
            text = stringResource(R.string.add_installed_app),
            onClickAction = {},
            painter = painterResource(R.drawable.ic_add)
        )
    }
}