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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.core.content.edit
import com.katdmy.android.bluetoothreadermusic.util.Constants.VOICE_NOTIFICATION_APPS
import com.katdmy.android.bluetoothreadermusic.util.ServiceHealthBus
import java.util.concurrent.atomic.AtomicInteger


class NotificationListener : NotificationListenerService() {

    //private val TAG = this.javaClass.simpleName
    private lateinit var listeningCommunicator: ListeningCommunicator
    private lateinit var tts: TextToSpeech
    private lateinit var audioManager: AudioManager
    private lateinit var focusRequest: AudioFocusRequest
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null
    private var lastReadNotificationText: String = ""
    private val prefs by lazy { getSharedPreferences("service_state", MODE_PRIVATE) }
    private var lastSavedHeartbeat = 0L
    private val queueCounter = AtomicInteger(0)
    private var lastTtsStartTime = 0L
    private val TTS_TIMEOUT = 5 * 60 * 1000L    // 5 минут

    @Volatile
    private var ttsReady: Boolean = false
    @Volatile
    private var useTTSCached: Boolean = true
    @Volatile
    private var ttsModeCached: Int = 0
    @Volatile
    private var randomVoiceCached: Boolean = false
    @Volatile
    private var enabledAppSetCached: Set<String> = setOf()



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val intent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationPosted")
        intent.putExtra("Data", "${getCurrentTime()} - service started")
        sendBroadcast(intent)

        return START_STICKY
    }

    override fun onListenerConnected() {
        val intent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationPosted")
        intent.putExtra("Data", "${getCurrentTime()} - listener connected")
        sendBroadcast(intent)

        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) ttsInitialized()
        }

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        focusRequest =
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).run {
                setAudioAttributes(AudioAttributes.Builder().run {
                    setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    build()
                })
                build()
            }
        listeningCommunicator = ListeningCommunicator()

        serviceScope.launch {
            launch {
                BTRMDataStore.getValueFlow(USE_TTS_SF, this@NotificationListener)
                    .collectLatest { useTTS ->
                        useTTSCached = useTTS == true
                        switchTTS(useTTSCached)
                    }
            }
            launch {
                BTRMDataStore.getValueFlow(TTS_MODE, this@NotificationListener)
                    .collectLatest { ttsMode ->
                        ttsModeCached = ttsMode ?: -1
                }
            }
            launch {
                BTRMDataStore.getValueFlow(RANDOM_VOICE, this@NotificationListener)
                    .collectLatest { randomVoice ->
                        randomVoiceCached = randomVoice == true
                    }
            }
            launch {
                BTRMDataStore.getValueFlow(VOICE_NOTIFICATION_APPS, this@NotificationListener)
                    .collectLatest { enabledAppsList ->
                        enabledAppSetCached = enabledAppsList?.getList()?.toSet() ?: setOf()
                    }
            }
            launch {
                while (isActive) {
                    val now = System.currentTimeMillis()

                    if (queueCounter.get() > 0 && now - lastTtsStartTime > TTS_TIMEOUT) {
                        restartTTS("tts is speaking > $TTS_TIMEOUT, restarting")
                    }

                    delay(10_000L)
                }
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
        audioManager.abandonAudioFocusRequest(focusRequest)
        queueCounter.set(0)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                lastTtsStartTime = System.currentTimeMillis()
                audioManager.requestAudioFocus(focusRequest)
            }

            override fun onDone(utteranceId: String) {
                lastTtsStartTime = System.currentTimeMillis()
                audioManager.abandonAudioFocusRequest(focusRequest)
                queueCounter.decrementAndGet()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                audioManager.abandonAudioFocusRequest(focusRequest)
                queueCounter.decrementAndGet()
            }
        })
    }

    override fun onDestroy() {
        val intent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationPosted")
        intent.putExtra("Data", "${getCurrentTime()} - service destroyed")
        sendBroadcast(intent)

        unregisterReceiver(listeningCommunicator)
        super.onDestroy()
    }

    fun switchTTS(newUseTTS: Boolean) {
        if (!newUseTTS) {
            tts.stop()
        } else {
            restartTTS()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {

        val packageName = sbn?.packageName
        val sortKey = sbn?.notification?.sortKey
        val key = sbn?.key
        val notificationTitle = sbn?.notification?.extras?.getCharSequence("android.title")
        val notificationText = sbn?.notification?.extras?.getCharSequence("android.text")
        val text = "$notificationTitle. $notificationText"

        when (ttsModeCached) {
            0 -> {
                if (packageName != applicationContext.packageName) {
                    val intent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationPosted")
                    intent.putExtra("Data", text)
                    sendBroadcast(intent)

                    readTTS(text)
                }
            }

            1 -> {
                if (packageName in enabledAppSetCached) {
                    when (packageName) {
                        "com.whatsapp" -> if (sortKey?.toInt() == 1) {
                            val intent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationPosted")
                            intent.putExtra("Data", text)
                            sendBroadcast(intent)

                            readTTS(text)
                        }
                        "com.instagram.android" -> if (key?.contains("|direct|") == true) readTTS(text)
                        "org.telegram.messenger" -> {
                            val intent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationPosted")
                            intent.putExtra("Data", "$sortKey - $key - $text")
                            sendBroadcast(intent)

                            readTTS(text)
                        }

                        else -> {
                            val intent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationPosted")
                            intent.putExtra("Data", text)
                            sendBroadcast(intent)

                            readTTS(text)
                        }
                    }
                }
            }

            else -> {}
        }
    }

    private fun readTTS(text: String?) {
        if (useTTSCached) {
            if (text != lastReadNotificationText && text != null) {
                lastReadNotificationText = text
                if (randomVoiceCached && tts.voices != null) {
                    val availableVoices = tts.voices.filter {
                        it.locale.language == Locale.getDefault().language
                    }
                    if (availableVoices.isNotEmpty())
                        tts.voice = availableVoices.random()
                }
                ttsTrySpeak(text)
            }
        }
    }

    private fun ttsTrySpeak(text: String) {
        if (!ttsReady)
            return

        queueCounter.incrementAndGet()
        if (queueCounter.get() > 20) {
            tts.stop()
            queueCounter.set(0)
        }
        val utteranceId = System.nanoTime().toString()
        val result = tts.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)

        if (result == TextToSpeech.ERROR) {
            restartTTS(text)

            tts.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
        }
    }

    private fun restartTTS(log: String = "") {
        val intent = Intent("com.katdmy.android.bluetoothreadermusic.onNotificationPosted")
        intent.putExtra("Data", "TTS ERROR: $log")
        try {
            tts.stop()
            tts.setOnUtteranceProgressListener(null)
            tts.shutdown()
        } catch (e: Exception) {
            intent.putExtra("Data", "TTS ERROR: ${e.localizedMessage}")
        }
        sendBroadcast(intent)

        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) ttsInitialized()
        }
    }

    private fun getCurrentTime(): String {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        return current.format(formatter)
    }


    inner class ListeningCommunicator : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            val command = intent.action?.split(".")?.last()
            when(command) {
                "onNotificationStopTTSClick" -> {
                    serviceScope.launch {
                        BTRMDataStore.saveValue(false, USE_TTS_SF, this@NotificationListener)
                    }
                }
                "onNotificationStartTTSClick" -> {
                    serviceScope.launch {
                        BTRMDataStore.saveValue(true, USE_TTS_SF, this@NotificationListener)
                    }
                }
                "abandonAudiofocus" -> {
                    //audioManager.abandonAudioFocusRequest(focusRequest)
                    restartTTS()
                }
            }
        }
    }
}