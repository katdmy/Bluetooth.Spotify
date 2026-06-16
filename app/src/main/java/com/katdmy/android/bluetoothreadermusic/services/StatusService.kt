package com.katdmy.android.bluetoothreadermusic.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.katdmy.android.bluetoothreadermusic.data.ServiceStatus
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.ui.views.ComposeActivity
import com.katdmy.android.bluetoothreadermusic.util.BTRMDataStore
import com.katdmy.android.bluetoothreadermusic.util.Constants.SERVICE_LAST_HEARTBEAT
import com.katdmy.android.bluetoothreadermusic.util.Constants.USE_TTS_SF
import com.katdmy.android.bluetoothreadermusic.util.PrefsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.core.content.edit
import com.katdmy.android.bluetoothreadermusic.data.models.NotificationUiState
import com.katdmy.android.bluetoothreadermusic.util.DebugLog
import com.katdmy.android.bluetoothreadermusic.util.ServiceHealthBus
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class StatusService: Service() {

    private val FOREGROUND_NOTIFICATION_ID = 10001
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var prefs: SharedPreferences
    private var lastSavedHeartbeat = 0L

    @Volatile
    private var useTTSCached: Boolean = true

    companion object {
        val serviceHealth = MutableStateFlow(ServiceStatus.Dead)
    }

    override fun onCreate() {
        super.onCreate()
        DebugLog.add("StatusService.onCreate")

        val useTtsFlow = BTRMDataStore
            .getValueFlow(USE_TTS_SF, this)
            .map { it == true }
            .distinctUntilChanged()

        prefs = PrefsHelper.getPrefs(this)

        createNotificationChannel()

        if (!startForegroundOnce()) {
            stopSelf()
            return
        }

        serviceScope.launch {
            combine(
                serviceHealth,
                useTtsFlow
            ) { health, useTTS ->
                useTTSCached = useTTS == true
                buildUiState(health)
            }
                .distinctUntilChanged()
                .collect { uiState ->
                    withContext(Dispatchers.Main) {
                        updateNotification(uiState)
                    }
                }
        }

        observeHeartbeat()
        startWatchdog()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        DebugLog.add("StatusService.onStartCommand intent=${intent?.action}")
        return START_STICKY
    }

    override fun onDestroy() {
        DebugLog.add("StatusService.onDestroy")
        serviceScope.cancel()
        super.onDestroy()
    }


    private fun buildUiState(
        health: ServiceStatus
    ): NotificationUiState {
        return when {
            health == ServiceStatus.Dead -> {
                NotificationUiState(
                    title = getText(R.string.notification_title_error),
                    icon = R.drawable.ic_warning,
                    actionIntent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationRestartListener"),
                    label = getString(R.string.restartService)
                )
            }
            !useTTSCached -> {
                NotificationUiState(
                    title = getText(R.string.notification_title_tts_off),
                    icon = R.drawable.ic_outline_notifications,
                    actionIntent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationStartTTSClick"),
                    label = getString(R.string.startTTS)
                )
            }
            else -> {
                NotificationUiState(
                    title = getText(R.string.notification_title_tts_on),
                    icon = R.drawable.ic_notifications,
                    actionIntent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationStopTTSClick"),
                    label = getString(R.string.stopTTS)
                )
            }
        }
    }

    private fun updateNotification(uiState: NotificationUiState) {
        val notification = buildNotification(uiState)
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun startForegroundOnce(): Boolean {
        return try {
            val initialNotification = buildNotification(buildUiState(ServiceStatus.Disabled))
            ServiceCompat.startForeground(
                this@StatusService,  // service
                FOREGROUND_NOTIFICATION_ID,  // id
                initialNotification,  //notification
                FOREGROUND_SERVICE_TYPE_MANIFEST
            )
            true
        } catch (e: Exception) {
            DebugLog.add("Error with status service: ${e.localizedMessage}")
            false
        }
    }

    private fun startWatchdog() {
        serviceScope.launch {
            ServiceHealthBus.heartbeatFlow.collect { timestamp ->
                lastSavedHeartbeat = timestamp
                serviceHealth.value = ServiceStatus.Working
            }
        }
    }

    private fun observeHeartbeat() {
        serviceScope.launch {
            while(isActive) {
                delay(120_000.milliseconds)

                val newState = if (!isListenerAlive()) ServiceStatus.Dead else serviceHealth.value
                if (newState != serviceHealth.value) {
                    serviceHealth.emit(newState)
                }

                prefs.edit { putLong(SERVICE_LAST_HEARTBEAT, lastSavedHeartbeat) }
            }
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


    fun buildNotification(uiState: NotificationUiState): Notification {
        //Log.e("ListenerStatusService", "createNotification")

        val openActivityPendingIntent: PendingIntent =
            Intent(this, ComposeActivity::class.java).let { openActivityIntent ->
                PendingIntent.getActivity(this, 0, openActivityIntent, PendingIntent.FLAG_IMMUTABLE)
            }

        val switchTTSPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, uiState.actionIntent, PendingIntent.FLAG_IMMUTABLE)
        val switchTTSAction: Notification.Action = Notification.Action.Builder(
            null,
            uiState.label,
            switchTTSPendingIntent
        )
            .build()

        val foregroundNotification = Notification.Builder(this, "notification_reader_service")
            .setContentTitle(uiState.title)
            .setSmallIcon(uiState.icon)
            .setContentIntent(openActivityPendingIntent)
            .addAction(switchTTSAction)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        return foregroundNotification
    }

    private fun createNotificationChannel() {
        val chan = NotificationChannel(
            "notification_reader_service",
            getString(R.string.service_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        val service = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
    }

    private fun isListenerAlive(): Boolean {
        val now = System.currentTimeMillis()

        // если процесс жив - используем память
        if (lastSavedHeartbeat != 0L) {
            return now - lastSavedHeartbeat < 30_000
        }

        // если процесс перезапущен - prefs
        val persisted = prefs.getLong(SERVICE_LAST_HEARTBEAT, 0L)
        return now - persisted < 30_000
    }
}