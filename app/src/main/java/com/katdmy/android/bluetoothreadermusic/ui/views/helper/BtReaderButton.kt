package com.katdmy.android.bluetoothreadermusic.ui.views.helper

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun BtReaderButton(
    text: String,
    onClickAction: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    ElevatedButton(
        onClick = onClickAction,
        contentPadding = PaddingValues(),
        enabled = enabled,
        modifier = modifier.defaultMinSize(
            minWidth = ButtonDefaults.MinWidth
        )
    ) {
        Row {
            icon?.let {
                Icon(
                    imageVector = icon,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(4.dp))
            }
            Text(
                text = text,
                maxLines = 1,
                modifier = Modifier.align(alignment = Alignment.CenterVertically)
            )
        }
    }
}