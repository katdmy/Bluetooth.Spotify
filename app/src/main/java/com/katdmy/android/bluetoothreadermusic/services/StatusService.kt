package com.katdmy.android.bluetoothreadermusic.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.notification.StatusBarNotification
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
import com.katdmy.android.bluetoothreadermusic.util.ServiceHealthBus
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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

        val useTtsFlow = BTRMDataStore
            .getValueFlow(USE_TTS_SF, this)
            .map { it == true }
            .distinctUntilChanged()

        prefs = PrefsHelper.getPrefs(this)

        startForegroundOnce()

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

        observeHearbeat()
        startWatchdog()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (intent?.action == "com.katdmy.android.bluetoothreadermusic.ACTION_NOTIFICATION_DISMISSED") {
            handleNotificationDismiss()
            return START_STICKY
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        serviceScope.cancel() // ← Важно! Иначе утечка корутин
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

    private fun startForegroundOnce() {
        val initialNotification = buildNotification(buildUiState(ServiceStatus.Disabled))
        ServiceCompat.startForeground(
            this@StatusService,  // service
            FOREGROUND_NOTIFICATION_ID,  // id
            initialNotification,  //notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                FOREGROUND_SERVICE_TYPE_MANIFEST
            }
        )
    }

    private fun startWatchdog() {
        serviceScope.launch {
            ServiceHealthBus.heartbeatFlow.collect { timestamp ->
                lastSavedHeartbeat = timestamp
                serviceHealth.value = ServiceStatus.Working
            }
        }
    }

    private fun observeHearbeat() {
        serviceScope.launch {
            while(isActive) {
                delay(120_000)

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

        createNotificationChannel()

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

        val deleteIntent = Intent(this, StatusService::class.java).apply {
            action = "com.katdmy.android.bluetoothreadermusic.ACTION_NOTIFICATION_DISMISSED"
        }

        val deletePendingIntent = PendingIntent.getService(
            this,
            1,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val foregroundNotification = Notification.Builder(this, "notification_reader_service")
            .setContentTitle(uiState.title)
            .setSmallIcon(uiState.icon)
            .setContentIntent(openActivityPendingIntent)
            .setDeleteIntent(deletePendingIntent)
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

    private fun isNotificationActive() : Boolean {
        //Log.e("NotificationListener", "isNotificationActive")
        val notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val activeNotifications: Array<StatusBarNotification> = notificationManager.activeNotifications
        for (notification in activeNotifications) {
            if (notification.id == FOREGROUND_NOTIFICATION_ID)
                return true
        }
        return false
    }

    private fun handleNotificationDismiss() {
        val now = System.currentTimeMillis()
        val lastDismiss = prefs.getLong("last_dismiss_time", 0L)

        prefs.edit { putLong("last_dismiss_time", now) }

        // если прошло больше 10 секунд - считаем случайным
        if (now - lastDismiss > 5_000) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isNotificationActive()) {
                    val notification = buildNotification(buildUiState(serviceHealth.value))
                    startForeground(FOREGROUND_NOTIFICATION_ID, notification)
                }
            }, 350)
        } else {
            // второй dismiss подряд - считаем намеренным
            stopSelf()
        }
    }
}

