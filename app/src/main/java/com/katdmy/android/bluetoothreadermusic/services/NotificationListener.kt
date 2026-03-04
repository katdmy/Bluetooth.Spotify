package com.katdmy.android.bluetoothreadermusic.services

import android.annotation.SuppressLint
import android.content.*
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
//import android.util.Log
import com.katdmy.android.bluetoothreadermusic.util.BTRMDataStore
import com.katdmy.android.bluetoothreadermusic.util.Constants.RANDOM_VOICE
import com.katdmy.android.bluetoothreadermusic.util.Constants.SERVICE_LAST_HEARTBEAT
import com.katdmy.android.bluetoothreadermusic.util.Constants.TTS_MODE
import com.katdmy.android.bluetoothreadermusic.util.Constants.USE_TTS_SF
import com.katdmy.android.bluetoothreadermusic.util.StringListHelper.getList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.core.content.edit
import com.katdmy.android.bluetoothreadermusic.util.BTRMDataStore.getValue
import com.katdmy.android.bluetoothreadermusic.util.Constants.VOICE_NOTIFICATION_APPS
import com.katdmy.android.bluetoothreadermusic.util.ServiceHealthBus


class NotificationListener : NotificationListenerService() {

    //private val TAG = this.javaClass.simpleName
    private lateinit var listeningCommunicator: ListeningCommunicator
    private lateinit var tts: TextToSpeech
    private lateinit var audioManager: AudioManager
    private lateinit var focusRequest: AudioFocusRequest
    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null
    private val mutex = Mutex()
    private var lastReadNotificationText: String = ""
    private var lastTelegramSortKey: Long = 0L
    private val prefs by lazy { getSharedPreferences("service_state", MODE_PRIVATE) }
    private var lastSavedHeartbeat = 0L


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //Log.e("NotificationListener", "onStartCommand")
        val intent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationPosted")
        intent.putExtra("Data", "${getCurrentTime()} - service started")
        sendBroadcast(intent)

        return START_STICKY
    }

    override fun onListenerConnected() {
        //Log.e("NotificationListener", "onListenerConnected")
        val intent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationPosted")
        intent.putExtra("Data", "${getCurrentTime()} - listener connected")
        sendBroadcast(intent)

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

        CoroutineScope(Dispatchers.IO).launch {
            BTRMDataStore.getValueFlow(USE_TTS_SF, this@NotificationListener).collectLatest { useTTS ->
                switchTTS(useTTS == true)
            }
        }

        val notificationsIntentFilter = IntentFilter().apply {
            addAction("com.katdmy.android.bluetoothreadermusic.onNotificationStopTTSClick")
            addAction("com.katdmy.android.bluetoothreadermusic.onNotificationStartTTSClick")
            addAction("com.katdmy.android.bluetoothreadermusic.abandonAudiofocus")
        }
        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(listeningCommunicator, notificationsIntentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(listeningCommunicator, notificationsIntentFilter)
        }

        startHeartbeat()
    }

    override fun onListenerDisconnected() {
        val intent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationPosted")
        intent.putExtra("Data", "${getCurrentTime()} - listener disconnected")
        sendBroadcast(intent)
        // TODO: не выполнять requestRebind в случае, когда сервис закрыт по команде пользователя или при прибитии приложения
        requestRebind(ComponentName(this, NotificationListener::class.java))
        super.onListenerDisconnected()
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            try {
                while (isActive) {
                    ServiceHealthBus.emitHeartbeat()

                    val now = System.currentTimeMillis()
                    if (now - lastSavedHeartbeat > 2 * 60_000) {
                        prefs.edit {
                            putLong(SERVICE_LAST_HEARTBEAT, System.currentTimeMillis())
                        }
                        lastSavedHeartbeat = now
                    }

                    delay(20_000)
                }
            } catch (_: Throwable) {}
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
        //Log.e("NotificationListener", "onDestroy")
        val intent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationPosted")
        intent.putExtra("Data", "${getCurrentTime()} - service destroyed")
        sendBroadcast(intent)

        if (scope.isActive) {
            scope.cancel()
        }
        unregisterReceiver(listeningCommunicator)
        super.onDestroy()
    }

    fun switchTTS(newUseTTS: Boolean) {
        //Log.e("NotificationListener", "switchTTS")
        if (scope.isActive) {
            scope.cancel()
        }

        if (!newUseTTS) {
            tts.stop()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        //Log.e("NotificationListener", "onNotificationPosted")

        val packageName = sbn?.packageName
        val sortKey = sbn?.notification?.sortKey
        val key = sbn?.key
        val title = sbn?.notification?.extras?.getCharSequence("android.title")
        val text = sbn?.notification?.extras?.getCharSequence("android.text")

        if (!scope.isActive) scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val useTTS = getValue(USE_TTS_SF, this@NotificationListener)

            if (useTTS == true) {
                val ttsMode = getValue(TTS_MODE, this@NotificationListener)
                when (ttsMode) {
                    0 -> {
                        if (packageName != applicationContext.packageName) {
                            mutex.withLock {
                                readTTS(title, text)
                            }
                        }
                    }

                    1 -> {
                        val enabledMessengersList = getValue(VOICE_NOTIFICATION_APPS, this@NotificationListener)
                                ?.getList() ?: listOf()
                        if (packageName in enabledMessengersList) {

                            val intent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationPosted")
                            intent.putExtra("Data", "$title - $text")
                            sendBroadcast(intent)

                            when (packageName) {
                                "com.whatsapp" -> if (sortKey?.toInt() == 1) readTTS(title, text)
                                "com.instagram.android" -> if (key?.contains("|direct|") == true) readTTS(title, text)
                                "org.telegram.messenger" -> if (sortKey != null && sortKey.toLong() > lastTelegramSortKey) {
                                    readTTS(title, text)
                                    lastTelegramSortKey = sortKey.toLong()
                                }

                                else -> readTTS(title, text)
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun readTTS(title: CharSequence?, text: CharSequence?) {
        if ("$title - $text" != lastReadNotificationText && (title != null || text != null)) {
            lastReadNotificationText = "$title - $text"
            scope.launch {
                val randomVoice = getValue(RANDOM_VOICE, this@NotificationListener)
                if (randomVoice == true) {
                    if (tts.voices == null) {
                        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "$title - $text")
                    } else {
                        tts.voice = tts.voices.filter { it.locale.language == Locale.getDefault().language }.random()
                        tts.speak(title, TextToSpeech.QUEUE_ADD, null, title.toString())
                        tts.voice = tts.voices.filter { it.locale.language == Locale.getDefault().language }.random()
                        tts.speak(text, TextToSpeech.QUEUE_ADD, null, text.toString())
                    }
                } else {
                    tts.speak("$title - $text", TextToSpeech.QUEUE_ADD, null, "$title - $text")
                }
            }
        }
    }

    private fun getCurrentTime(): String {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        return current.format(formatter)
    }


    inner class ListeningCommunicator : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            //Log.e("ListeningCommunicator", "onReceive")

            val command = intent.action?.split(".")?.last()
            //Log.e(TAG, "command received:  $command")
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
                "abandonAudiofocus" -> {
                    audioManager.abandonAudioFocusRequest(focusRequest)
                }
            }
        }
    }
}