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
import com.katdmy.android.bluetoothreadermusic.util.Constants.ENABLED_MESSENGERS
import com.katdmy.android.bluetoothreadermusic.util.Constants.SERVICE_STARTED
import com.katdmy.android.bluetoothreadermusic.util.Constants.TTS_MODE
import com.katdmy.android.bluetoothreadermusic.util.Constants.USE_TTS_SF
import com.katdmy.android.bluetoothreadermusic.util.StringListHelper.getList
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
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private var lastReadNotificationText: String = ""


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

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
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
            BTRMDataStore.getValueFlow(USE_TTS_SF, this@NotificationListener).collectLatest {
                useTTS -> createNotification(useTTS == true)
            }
        }

        val notificationsIntentFilter = IntentFilter().apply {
            addAction("com.katdmy.android.bluetoothreadermusic.onNotificationStopTTSClick")
            addAction("com.katdmy.android.bluetoothreadermusic.onNotificationStartTTSClick")
            addAction("com.katdmy.android.bluetoothreadermusic.stopServiceIntentClick")
            addAction("com.katdmy.android.bluetoothreadermusic.getStatus")
            addAction("com.katdmy.android.bluetoothreadermusic.abandonAudiofocus")
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
            override fun onError(utteranceId: String) {
                audioManager.abandonAudioFocusRequest(focusRequest)
            }
        })
    }

    override fun onDestroy() {
        Log.e("NotificationListener", "onDestroy")
        if (scope.isActive) {
            scope.cancel()
        }
        unregisterReceiver(listeningCommunicator)
        super.onDestroy()
    }

    fun switchTTS(newUseTTS: Boolean) {
        Log.e("NotificationListener", "switchTTS")
        if (scope.isActive) {
            scope.cancel()
        }

        if (!newUseTTS) {
            tts.stop()
        }

        createNotification(newUseTTS)

//        if (status)
//            openMusic()
    }

    private fun isNotificationActive() : Boolean {
        Log.e("NotificationListener", "isNotificationActive")
        val notificationManager = this@NotificationListener.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val activeNotifications: Array<StatusBarNotification> = notificationManager.activeNotifications
        for (notification in activeNotifications) {
            if (notification.id == FOREGROUND_NOTIFICATION_ID)
                return true
        }
        return false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        Log.e("NotificationListener", "onNotificationPosted")

        val packageName = sbn?.packageName
        val key = sbn?.key
        val title = sbn?.notification?.extras?.getCharSequence("android.title")
        val text = sbn?.notification?.extras?.getCharSequence("android.text")
        val sortKey = sbn?.notification?.sortKey
        val data = "$title - $text"

        val intent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationPosted")
        intent.putExtra("Package Name", packageName)
        intent.putExtra("Key", key)
        intent.putExtra("Title", title)
        intent.putExtra("Text", text)

        if (!scope.isActive) scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val useTTS = BTRMDataStore.getValue(USE_TTS_SF, this@NotificationListener)
            if (!isNotificationActive())
                createNotification(useTTS == true)

            if (useTTS == true) {
                val ttsMode = BTRMDataStore.getValue(TTS_MODE, this@NotificationListener)
                when (ttsMode) {
                    0 -> {
                        // TODO: Необходимо исключить повторяющиеся уведомления и постоянные от всяких VPN
                        if (packageName != applicationContext.packageName)
                            readTTS(data)
                        intent.putExtra("Data", "$packageName - $sortKey - $key - $title - $text")
                        sendBroadcast(intent)
                    }

                    1 -> {
                        val enabledMessengersList =
                            BTRMDataStore.getValue(ENABLED_MESSENGERS, this@NotificationListener)
                                ?.getList() ?: listOf()
                        if (packageName in enabledMessengersList) {
                            when (packageName) {
                                "com.whatsapp" -> if (sortKey == "1") readTTS(data)
                                "com.instagram.android" -> if (key?.contains("|direct|") == true) readTTS(data)
                                else -> readTTS(data)
                            }
                            intent.putExtra("Data", "$packageName - $sortKey - $key - $title - $text")
                            sendBroadcast(intent)
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun readTTS(text: String) {
        if (text != lastReadNotificationText) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, text)
            lastReadNotificationText = text
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
            .setSmallIcon(icon)
            .setContentIntent(openActivityPendingIntent)
            .addAction(switchTTSAction)
            .addAction(stopServiceAction)
            .setDeleteIntent(dismissedPendingIntent)
            .build()

        val mNotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
        val service = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
    }


    inner class ListeningCommunicator : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            Log.e("ListeningCommunicator", "onReceive")

            val command = intent.action?.split(".")?.last()
            Log.e(TAG, "command received:  $command")
            when(command) {
                "onNotificationStopTTSClick" -> {
                    if (!scope.isActive) scope = CoroutineScope(Dispatchers.IO)
                    scope.launch {
                        BTRMDataStore.saveValue(false, USE_TTS_SF, this@NotificationListener)
                    }
                }
                "onNotificationStartTTSClick" -> {
                    if (!scope.isActive) scope = CoroutineScope(Dispatchers.IO)
                    scope.launch {
                        BTRMDataStore.saveValue(true, USE_TTS_SF, this@NotificationListener)
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
                    scope.cancel()
                }
                "abandonAudiofocus" -> {
                    audioManager.abandonAudioFocusRequest(focusRequest)
                }
                "DISMISSED_ACTION" -> {
                    if (!scope.isActive) scope = CoroutineScope(Dispatchers.IO)
                    scope.launch {
                        val useTTS = BTRMDataStore.getValue(USE_TTS_SF, this@NotificationListener)
                        switchTTS(useTTS == true)
                    }
                }
                "CONNECTION_STATE_CHANGED" -> {
                    when (intent.extras?.getInt(BluetoothProfile.EXTRA_STATE)) {
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            if (!scope.isActive) scope = CoroutineScope(Dispatchers.IO)
                            scope.launch {
                                BTRMDataStore.saveValue(false, USE_TTS_SF, this@NotificationListener)
                            }
                        }
                        BluetoothProfile.STATE_CONNECTED -> {
                            if (!scope.isActive) scope = CoroutineScope(Dispatchers.IO)
                            scope.launch {
                                BTRMDataStore.saveValue(true, USE_TTS_SF, this@NotificationListener)
                            }
                        }
                    }
                }
            }
        }
    }
}