package com.katdmy.android.bluetoothreadermusic.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.service.notification.StatusBarNotification
import androidx.core.app.ServiceCompat
import com.katdmy.android.bluetoothreadermusic.data.NotificationState
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.ui.views.ComposeActivity
import com.katdmy.android.bluetoothreadermusic.util.BTRMDataStore
import com.katdmy.android.bluetoothreadermusic.util.Constants.USE_TTS_SF
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ListenerStatusService: Service() {

    private val FOREGROUND_NOTIFICATION_ID = 10001
    private lateinit var statusServiceCommunicator: StatusServiceCommunicator
    private lateinit var currentState: NotificationState
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        statusServiceCommunicator = StatusServiceCommunicator()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        val intentFilter = IntentFilter().apply {
            addAction("com.katdmy.android.bluetoothreadermusic.stopStatusService")
        }
        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusServiceCommunicator, intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(statusServiceCommunicator, intentFilter)
        }

        updateNotificationState()
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this@ListenerStatusService,  // service
                FOREGROUND_NOTIFICATION_ID,  // id
                notification,  //notification
                FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            ServiceCompat.startForeground(
                this@ListenerStatusService,  // service
                FOREGROUND_NOTIFICATION_ID,  // id
                notification,  //notification
                FOREGROUND_SERVICE_TYPE_MANIFEST
            )
        }

        startChecking()

        if (!scope.isActive) scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            BTRMDataStore.getValueFlow(USE_TTS_SF, this@ListenerStatusService).collectLatest { _ ->
                stopChecking()
                startChecking()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(statusServiceCommunicator)
        } catch (e: IllegalArgumentException) {
            //Log.e("ListenerStatusService", "Receiver was not registered", e)
        }
        stopChecking()
        if (scope.isActive) {
            scope.cancel()
        }
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


    fun createNotification(): Notification {
        //Log.e("ListenerStatusService", "createNotification")

        var switchTTSIntent: Intent
        var notificationActionButtonLabel: String
        var contentTitle: CharSequence
        var icon: Int

        when (currentState) {
            NotificationState.ListenerError -> {
                switchTTSIntent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationRestartListener")
                notificationActionButtonLabel = getString(R.string.restartService)
                contentTitle = getText(R.string.notification_title_error)
                icon = R.drawable.ic_warning
            }
            NotificationState.NoUseTTS -> {
                switchTTSIntent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationStartTTSClick")
                notificationActionButtonLabel = getString(R.string.startTTS)
                contentTitle = getText(R.string.notification_title_tts_off)
                icon = R.drawable.ic_outline_notifications
            }
            NotificationState.UseTTS -> {
                switchTTSIntent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationStopTTSClick")
                notificationActionButtonLabel = getString(R.string.stopTTS)
                contentTitle = getText(R.string.notification_title_tts_on)
                icon = R.drawable.ic_notifications
            }
        }

        createNotificationChannel()

        val openActivityPendingIntent: PendingIntent =
            Intent(this, ComposeActivity::class.java).let { openActivityIntent ->
                PendingIntent.getActivity(this, 0, openActivityIntent, PendingIntent.FLAG_IMMUTABLE)
            }

        val switchTTSPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, switchTTSIntent, PendingIntent.FLAG_IMMUTABLE)
        val switchTTSAction: Notification.Action = Notification.Action.Builder(
            null,
            notificationActionButtonLabel,
            switchTTSPendingIntent
        )
            .build()

        val stopServiceIntent =
            Intent("com.katdmy.android.bluetoothreadermusic.stopServiceIntentClick")
        val stopServicePendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, stopServiceIntent, PendingIntent.FLAG_IMMUTABLE)
        val stopServiceAction: Notification.Action = Notification.Action.Builder(
            null,
            getString(R.string.stopService),
            stopServicePendingIntent
        )
            .build()

        val foregroundNotification = Notification.Builder(this, "notification_reader_service")
            .setContentTitle(contentTitle)
            .setSmallIcon(icon)
            .setContentIntent(openActivityPendingIntent)
            .addAction(switchTTSAction)
            .addAction(stopServiceAction)
            .build()

        return foregroundNotification
    }

    private fun postForegroundNotification(foregroundNotification: Notification) {
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(FOREGROUND_NOTIFICATION_ID, foregroundNotification)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                foregroundNotification,
                FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                foregroundNotification
            )
        }
    }

    private fun createNotificationChannel() {
        val chan = NotificationChannel(
            "notification_reader_service",
            getString(R.string.service_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
    }


    private val checkRunnable = object : Runnable {
        override fun run() {
            val oldState = currentState
            updateNotificationState()

            if (currentState == NotificationState.ListenerError)
                restartNotificationService(this@ListenerStatusService)
            if (oldState != currentState || !isNotificationActive())
                postForegroundNotification(createNotification())

            handler.removeCallbacks(this)
            handler.postDelayed(this, 5000)
        }
    }

    fun startChecking() {
        handler.post(checkRunnable)
    }

    fun stopChecking() {
        handler.removeCallbacks(checkRunnable)
    }

    fun isNotificationServiceEnabled(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(context.packageName) == true
    }

    fun restartNotificationService(context: Context) {
        val componentName = ComponentName(context, NotificationListener::class.java)
        val pm = context.packageManager
        pm.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        Thread.sleep(500)
        pm.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun updateNotificationState() {
        if (!isNotificationServiceEnabled(this@ListenerStatusService)) {
            currentState = NotificationState.ListenerError
        } else
            runBlocking {
                val useTTS =
                    BTRMDataStore.getValue(USE_TTS_SF, this@ListenerStatusService) == true
                currentState = if (useTTS) NotificationState.UseTTS
                                else NotificationState.NoUseTTS
        }
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


    inner class StatusServiceCommunicator : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            val command = intent.action?.split(".")?.last()
            //Log.e("StatusServiceCommunicator", "command received:  $command")
            when (command) {
                "stopStatusService" -> {
                    this@ListenerStatusService.stopSelf()
                }

                else -> {

                }
            }
        }
    }
}

