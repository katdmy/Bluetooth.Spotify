package com.katdmy.android.bluetoothreadermusic.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.ui.vm.MainViewModel
import com.katdmy.android.bluetoothreadermusic.ui.theme.BtReaderMusicTheme
import com.katdmy.android.bluetoothreadermusic.util.BTRMDataStore
import com.katdmy.android.bluetoothreadermusic.util.Constants.USE_TTS_SF
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onComplete: () -> Unit,
    onChangeUseTTS: ((Boolean) -> Unit),
    onRequestReadNotificationsPermission: () -> Unit,
    onRequestShowNotificationPermission: () -> Unit,
    onRequestBtPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagesCount = 5
    val pagerState = rememberPagerState { pagesCount }
    val coroutineScope = rememberCoroutineScope()
    val permissionState = viewModel.permissionState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        HorizontalPager(
            state = pagerState
        ) { page ->
            when (page) {
                0 -> OnboardingPage(
                    title = stringResource(R.string.onboarding_welcome_title),
                    description = stringResource(R.string.onboarding_welcome_description),
                    imageRes = R.drawable.ic_welcome
                )

                1 -> OnboardingPage(
                    title = stringResource(R.string.onboarding_read_notifications_title),
                    description = stringResource(
                        R.string.onboarding_read_notifications_description,
                        getAppName()
                    ),
                    imageRes = R.drawable.ic_read_notifications,
                    permissionGranted = permissionState.value.readNotifications,
                    buttonText = stringResource(R.string.open_permissions_menu),
                    onButtonClick = onRequestReadNotificationsPermission
                )

                2 -> OnboardingPage(
                    title = stringResource(R.string.onboarding_tts_title),
                    description = stringResource(R.string.onboarding_tts_description),
                    imageRes = R.drawable.ic_tts,
                    onChangeUseTTS = onChangeUseTTS
                )

                3 -> OnboardingPage(
                    title = stringResource(R.string.onboarding_post_notification_title),
                    description = stringResource(R.string.onboarding_post_notification_description),
                    imageRes = R.drawable.ic_show_notification,
                    permissionGranted = permissionState.value.postNotification,
                    buttonText = stringResource(R.string.enable_permission),
                    onButtonClick = onRequestShowNotificationPermission
                )

                4 -> OnboardingPage(
                    title = stringResource(R.string.onboarding_bt_status_title),
                    description = stringResource(R.string.onboarding_bt_status_description),
                    imageRes = R.drawable.ic_bluetooth,
                    permissionGranted = permissionState.value.btStatus,
                    buttonText = stringResource(R.string.enable_permission),
                    onButtonClick = onRequestBtPermission
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.weight(1f))
            if (pagerState.currentPage > 0) {
                Button(onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(
                            pagerState.currentPage - 1
                        )
                    }
                }) {
                    Text(text = stringResource(R.string.onboarding_back))
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            if (pagerState.currentPage < pagesCount - 1) {
                Button(onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(
                            pagerState.currentPage + 1
                        )
                    }
                }) {
                    Text(text = stringResource(R.string.onboarding_next))
                }
            } else {
                Button(onClick = onComplete) {
                    Text(stringResource(R.string.onboarding_launch))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        OnboardingIndicator(
            totalPages = pagerState.pageCount,
            currentPage = pagerState.currentPage,
            modifier = Modifier.padding(bottom = 32.dp)
        )

    }
}

@Composable
fun OnboardingPage(
    title: String,
    description: String,
    imageRes: Int,
    modifier: Modifier = Modifier,
    permissionGranted: Boolean? = null,
    onChangeUseTTS: ((Boolean) -> Unit)? = null,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            modifier = Modifier.height(100.dp).width(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(description, textAlign = TextAlign.Center)

        permissionGranted?.let {
            if (permissionGranted) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .padding(vertical = 16.dp, horizontal = 36.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_permission_granted),
                        color = Color.Green)
                    Image(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = "Check",
                        colorFilter = ColorFilter.tint(Color.Green)
                    )
                }
            } else {
                onButtonClick?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = it) {
                        Text(buttonText!!)
                    }
                }
            }
        }
        onChangeUseTTS?.let {
            val context = LocalContext.current
            val useTTS = BTRMDataStore.getValueFlow(USE_TTS_SF, context).collectAsState(initial = false)
            Spacer(modifier = Modifier.height(16.dp))
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
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Switch(
                    checked = useTTS.value == true,
                    onCheckedChange = onChangeUseTTS
                )
            }
        }
    }
}

@Composable
fun getAppName(): String {
    val context = LocalContext.current
    val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
    return context.packageManager.getApplicationLabel(appInfo).toString()
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    BtReaderMusicTheme {
        OnboardingScreen(
            viewModel = MainViewModel(),
            onComplete = {},
            onChangeUseTTS = {},
            onRequestReadNotificationsPermission = {},
            onRequestShowNotificationPermission = {},
            onRequestBtPermission = {}
        )
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun OnboardingScreenPreviewInRussian() {
    BtReaderMusicTheme {
        OnboardingScreen(
            viewModel = MainViewModel(),
            onComplete = {},
            onChangeUseTTS = {},
            onRequestReadNotificationsPermission = {},
            onRequestShowNotificationPermission = {},
            onRequestBtPermission = {}
        )
    }
}


    @Composable
fun OnboardingIndicator(
    totalPages: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 .. (totalPages - 1)) {
            val isActive = i == currentPage
            val widthState = animateDpAsState(
                targetValue = if (isActive) 24.dp else 8.dp,
                animationSpec = tween(durationMillis = 300),
                label = "indicator_width"
            )
            val width = widthState.value
            val color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}