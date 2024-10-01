package com.katdmy.android.bluetoothreadermusic.services

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothProfile
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.katdmy.android.bluetoothreadermusic.R
import com.katdmy.android.bluetoothreadermusic.ui.ComposeActivity
import com.katdmy.android.bluetoothreadermusic.util.BTRMDataStore
import com.katdmy.android.bluetoothreadermusic.util.Constants.SERVICE_STARTED
import com.katdmy.android.bluetoothreadermusic.util.Constants.USE_TTS_SF
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class NotificationListener : NotificationListenerService() {

    private val TAG = this.javaClass.simpleName
    private val FOREGROUND_NOTIFICATION_ID = 10001
    private lateinit var listeningCommunicator: ListeningCommunicator
    private lateinit var tts: TextToSpeech
    private lateinit var audioManager: AudioManager
    private lateinit var focusRequest: AudioFocusRequest
    private var lastReadData = ""
    private var errorMessage = ""
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e("NotificationListener", "onStartCommand")
        return START_STICKY
    }

    override fun onListenerConnected() {
        Log.e("NotificationListener", "onListenerConnected")
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInitialized()
            }
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        focusRequest =
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE).run {
                setAudioAttributes(AudioAttributes.Builder().run {
                    setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    build()
                })
                build()
            }
        listeningCommunicator = ListeningCommunicator()

        if (!scope.isActive) scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            BTRMDataStore.getValueFlow(USE_TTS_SF, this@NotificationListener)
                .collectLatest { useTTS ->
                    createNotification(useTTS ?: false)
                }
        }

        val notificationsIntentFilter = IntentFilter().apply {
            addAction("com.katdmy.android.bluetoothreadermusic.onVoiceUseChange")
            addAction("com.katdmy.android.bluetoothreadermusic.onNotificationStopTTSClick")
            addAction("com.katdmy.android.bluetoothreadermusic.onNotificationStartTTSClick")
            addAction("com.katdmy.android.bluetoothreadermusic.stopServiceIntentClick")
            addAction("com.katdmy.android.bluetoothreadermusic.DISMISSED_ACTION")
            addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
        }
        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(listeningCommunicator, notificationsIntentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(listeningCommunicator, notificationsIntentFilter)
        }
    }

    private fun ttsInitialized() {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                audioManager.requestAudioFocus(focusRequest)
            }

            override fun onDone(utteranceId: String) {
                audioManager.abandonAudioFocusRequest(focusRequest)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(p0: String) {
                audioManager.abandonAudioFocusRequest(focusRequest)
            }
        })
    }

    override fun onDestroy() {
        Log.e("NotificationListener", "onDestroy")
        if (!scope.isActive) scope.cancel()
        unregisterReceiver(listeningCommunicator)
        super.onDestroy()
    }

    fun switchTTS(newUseTTS: Boolean) {
        Log.e("NotificationListener", "switchTTS")
        if (!newUseTTS) {
            tts.stop()
            if (!scope.isActive) scope.cancel()
        }

        createNotification(newUseTTS)

//        if (status)
//            openMusic()
    }

    /*private fun isNotificationActive() : Boolean {
        Log.e("NotificationListener", "isNotificationActive")
        val notificationManager = this@NotificationListener.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val activeNotifications: Array<StatusBarNotification> = notificationManager.activeNotifications
        for (notification in activeNotifications) {
            if (notification.id == FOREGROUND_NOTIFICATION_ID)
                return true
        }
        return false
    }*/

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        Log.e("NotificationListener", "onNotificationPosted")
        val intent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationPosted")
        intent.putExtra("Package Name", sbn?.packageName)
        intent.putExtra("Key", sbn?.key)
        intent.putExtra("Title", sbn?.notification?.extras?.getCharSequence("android.title"))
        intent.putExtra("Text", sbn?.notification?.extras?.getCharSequence("android.text"))
        //sendBroadcast(intent)

        if (!scope.isActive) scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            BTRMDataStore.getValueFlow(USE_TTS_SF, this@NotificationListener).collectLatest { useTTS ->

//                if (!isNotificationActive()) createNotification(useTTS ?: false)

                if (useTTS == true) {
                    val key = sbn?.key
                    //Log.e("NotificationListener", "Notification key: ${key}")
                    if (key?.contains("0|com.whatsapp|1") == true && !key.contains("0|com.whatsapp|1|null")
                    ) {
                        val title = intent.getStringExtra("Title") ?: ""
                        val text = intent.getStringExtra("Text") ?: ""
                        val data = "$title - $text"
                        if (data != lastReadData && sbn.notification?.sortKey == "1") {
                            tts.speak(data, TextToSpeech.QUEUE_ADD, null, data)
                            lastReadData = data
                            intent.putExtra("Data", data)
                            sendBroadcast(intent)
                        }
                    }
                }
            }
        }

    }

    fun createNotification(useTTS: Boolean) {
        Log.e("NotificationListener", "createNotification")
        createNotificationChannel()

        val openActivityPendingIntent: PendingIntent =
            Intent(this, ComposeActivity::class.java).let { openActivityIntent ->
                PendingIntent.getActivity(this, 0, openActivityIntent, PendingIntent.FLAG_IMMUTABLE)
            }

        // Need to change intent url in order to android can distinguish different intents
        val switchTTSIntent = if (useTTS) {
            Intent("com.katdmy.android.bluetoothreadermusic.onNotificationStopTTSClick")
        } else {
            Intent("com.katdmy.android.bluetoothreadermusic.onNotificationStartTTSClick")
        }
        val switchTTSPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, switchTTSIntent, PendingIntent.FLAG_IMMUTABLE)
        val switchTTSAction: Notification.Action = Notification.Action.Builder(
                null,
                if (useTTS) getString(R.string.stopTTS)
                else getString(R.string.startTTS),
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

        val dismissedIntent = Intent("com.katdmy.android.bluetoothreadermusic.DISMISSED_ACTION")
        dismissedIntent.setPackage(packageName)
        val dismissedPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, dismissedIntent, PendingIntent.FLAG_IMMUTABLE)

        val icon = if (useTTS) R.drawable.ic_notifications
        else R.drawable.ic_outline_notifications

        val foregroundNotification = Notification.Builder(this, "my_service")
            .setContentTitle(getText(R.string.notification_title))
            //.setContentText(getText(R.string.notification_message))
            //.setContentText("useTTS: $useTTS")
            .setStyle(Notification.BigTextStyle().bigText(errorMessage))
            .setSmallIcon(icon)
            .setContentIntent(openActivityPendingIntent)
            .addAction(switchTTSAction)
            .addAction(stopServiceAction)
            .setDeleteIntent(dismissedPendingIntent)
            .build()

        val mNotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(FOREGROUND_NOTIFICATION_ID, foregroundNotification)

        //stopForeground(STOP_FOREGROUND_REMOVE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(FOREGROUND_NOTIFICATION_ID, foregroundNotification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, foregroundNotification)
        }
    }

    private fun createNotificationChannel() {
        val chan = NotificationChannel(
            "my_service",
            "My Background Service",
            NotificationManager.IMPORTANCE_LOW
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
    }


    inner class ListeningCommunicator : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            Log.e("ListeningCommunicator", "onReceive")

            val command = intent.action?.split(".")?.last()
            Log.e(TAG, "command received:  $command")
            Log.e(TAG, "scope.isActive : ${scope.isActive}")
            when(command) {
                "onVoiceUseChange" -> {
                    if (!scope.isActive) scope = CoroutineScope(Dispatchers.IO)
                    scope.launch {
                        BTRMDataStore.getValueFlow(USE_TTS_SF, this@NotificationListener).collectLatest { useTTS ->
                            switchTTS(useTTS ?: false)
                        }
                    }
                }
                "onNotificationStopTTSClick" -> {
                    if (!scope.isActive) scope = CoroutineScope(Dispatchers.IO)
                    scope.launch {
                        BTRMDataStore.saveValue(false, USE_TTS_SF, this@NotificationListener)
                        BTRMDataStore.getValueFlow(USE_TTS_SF, this@NotificationListener).collectLatest { useTTS ->
                            switchTTS(useTTS ?: false)
                        }
                    }
                }
                "onNotificationStartTTSClick" -> {
                    if (!scope.isActive) scope = CoroutineScope(Dispatchers.IO)
                    scope.launch {
                        BTRMDataStore.saveValue(true, USE_TTS_SF, this@NotificationListener)
                        BTRMDataStore.getValueFlow(USE_TTS_SF, this@NotificationListener).collectLatest { useTTS ->
                            switchTTS(useTTS ?: false)
                        }
                    }
                }
                "stopServiceIntentClick" -> {
                    val pm = context.packageManager
                    pm.setComponentEnabledSetting(
                        ComponentName(context, NotificationListener::class.java),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    if (!scope.isActive) scope = CoroutineScope(Dispatchers.IO)
                    scope.launch {
                        BTRMDataStore.saveValue(false, SERVICE_STARTED, this@NotificationListener)
                    }
                }
                "showNotificationWithError" -> {
                    errorMessage = intent.getStringExtra("errorMessage") ?: ""

                    if (!scope.isActive) scope = CoroutineScope(Dispatchers.IO)
                    scope.launch {
                        BTRMDataStore.getValueFlow(USE_TTS_SF, this@NotificationListener).collectLatest { useTTS ->
                            if (useTTS == true) createNotification(useTTS)
                        }
                    }
                }
                "DISMISSED_ACTION" -> {
                    if (!scope.isActive) scope = CoroutineScope(Dispatchers.IO)
                    scope.launch {
                        BTRMDataStore.getValueFlow(USE_TTS_SF, this@NotificationListener).collectLatest { useTTS ->
                            if (useTTS == true) createNotification(useTTS)
                        }
                    }
                }
                "CONNECTION_STATE_CHANGED" -> {
                    when (intent.extras?.getInt(BluetoothProfile.EXTRA_STATE)) {
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            if (!scope.isActive) scope = CoroutineScope(Dispatchers.IO)
                            scope.launch {
                                BTRMDataStore.saveValue(false, USE_TTS_SF, this@NotificationListener)
                                BTRMDataStore.getValueFlow(USE_TTS_SF, this@NotificationListener).collectLatest { useTTS ->
                                    switchTTS(useTTS ?: false)
                                }
                            }
                        }
                        BluetoothProfile.STATE_CONNECTED -> {
                            if (!scope.isActive) scope = CoroutineScope(Dispatchers.IO)
                            scope.launch {
                                BTRMDataStore.saveValue(true, USE_TTS_SF, this@NotificationListener)
                                BTRMDataStore.getValueFlow(USE_TTS_SF, this@NotificationListener).collectLatest { useTTS ->
                                    switchTTS(useTTS ?: false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}