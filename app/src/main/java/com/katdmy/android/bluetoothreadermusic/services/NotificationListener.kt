package com.katdmy.android.bluetoothreadermusic.services

import android.annotation.SuppressLint
import android.app.Notification
import android.content.*
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
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
import java.util.Locale
import androidx.core.content.edit
import com.katdmy.android.bluetoothreadermusic.data.models.NotificationFingerprint
import com.katdmy.android.bluetoothreadermusic.util.Constants.TTS_VOLUME
import com.katdmy.android.bluetoothreadermusic.util.Constants.VOICE_NOTIFICATION_APPS
import com.katdmy.android.bluetoothreadermusic.util.DebugLog
import com.katdmy.android.bluetoothreadermusic.util.ServiceHealthBus
import java.util.concurrent.atomic.AtomicInteger


class NotificationListener : NotificationListenerService() {

    private lateinit var listeningCommunicator: ListeningCommunicator
    private lateinit var tts: TextToSpeech
    private lateinit var audioManager: AudioManager
    private lateinit var focusRequest: AudioFocusRequest
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null
    private var recent = LinkedHashSet<NotificationFingerprint>()
    private val prefs by lazy { getSharedPreferences("service_state", MODE_PRIVATE) }
    private var lastSavedHeartbeat = 0L
    private val queueCounter = AtomicInteger(0)
    private var lastTtsStartTime = 0L
    private val TTS_TIMEOUT = 5 * 60 * 1000L    // 5 минут

    @Volatile
    private var validRandomVoices: List<Voice> = emptyList()
    @Volatile
    private var currentVoice: Voice? = null
    private var defaultFallbackVoice: Voice? = null

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
    @Volatile
    private var volumeCached: Float = 1f



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onListenerConnected() {
        DebugLog.add(this, "Notification service connected")

        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) ttsInitialized()
            else DebugLog.add(this, "TTS initialization failed")
        }

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        focusRequest =
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).run {
                setAudioAttributes(AudioAttributes.Builder().run {
                    setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
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
                BTRMDataStore.getValueFlow(TTS_VOLUME, this@NotificationListener)
                    .collectLatest { volume ->
                        volumeCached = volume ?: 1f
                    }
            }
            launch {
                while (isActive) {
                    val now = System.currentTimeMillis()

                    if (queueCounter.get() > 0 && now - lastTtsStartTime > TTS_TIMEOUT) {
                        restartTTS()
                    }

                    delay(10_000L)
                }
            }
        }

        val notificationsIntentFilter = IntentFilter().apply {
            addAction("com.katdmy.android.bluetoothreadermusic.onNotificationStopTTSClick")
            addAction("com.katdmy.android.bluetoothreadermusic.onNotificationStartTTSClick")
            addAction("com.katdmy.android.bluetoothreadermusic.forceRestartTTS")
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
        DebugLog.add(this, "Notification service disconnected")
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

                    delay(10_000)
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
        DebugLog.add(this, "TTS initialized")

        refreshValidVoices()
    }

    override fun onDestroy() {
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

    private fun restartTTS() {
        try {
            tts.stop()
            tts.setOnUtteranceProgressListener(null)
            tts.shutdown()
        } catch (_: Exception) {}

        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) ttsInitialized()
        }
    }

    private fun refreshValidVoices() {
        val targetLang = Locale.getDefault().language
        val allVoices = tts.voices ?: run {
            DebugLog.add(this, "No valid voices installed for $targetLang")
            validRandomVoices = emptyList()
            defaultFallbackVoice = null
            return
        }

        val safeVoices = mutableListOf<Voice>()
        for (voice in allVoices) {
            if (voice.locale?.language == targetLang &&
                !voice.isNetworkConnectionRequired &&
                voice.quality >= Voice.QUALITY_NORMAL
            ) {
                try {
                    tts.voice = voice
                    safeVoices.add(voice)
                } catch (_: Exception) {}
            }
        }
        validRandomVoices = safeVoices
        defaultFallbackVoice = safeVoices.firstOrNull()

        DebugLog.add(this, "Voices loaded: ${validRandomVoices.size} for locale ${Locale.getDefault()}")
    }


    override fun onNotificationPosted(sbn: StatusBarNotification) {

        val packageName = sbn.packageName
        val sortKey = sbn.notification?.sortKey
        val key = sbn.key
        val extras = sbn.notification?.extras
        val aTitle = extras?.getCharSequence("android.title")
        val aText = extras?.getCharSequence("android.text")

        val bundles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras?.getParcelableArray(
                "android.messages",
                Bundle::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            extras?.getParcelableArray("android.messages")
        }
        var pSender = ""
        var pText = ""

        if (bundles != null) {
            val msg = Notification.MessagingStyle.Message
                .getMessagesFromBundleArray(bundles).last()

            pText = msg.text.toString()
            @Suppress("DEPRECATION")
            pSender = (
                    msg.senderPerson?.name
                        ?: msg.sender
                        ?: pSender
                    ).toString()
        }

        val title = pSender.ifBlank { aTitle }
        val text = pText.ifBlank { aText }

        if (useTTSCached && title?.isNotBlank() == true && text?.isNotBlank() == true && key != null) {

            val textToRead = "$title. $text"
            val fingerprint = NotificationFingerprint(key, textToRead)
            if (!recent.contains(fingerprint)) {
                recent.add(fingerprint)
                if (recent.size > 50) {
                    recent.remove(recent.first())
                }

                when (ttsModeCached) {
                    0 -> {
                        if (packageName != applicationContext.packageName)
                            readTTS(textToRead)
                    }

                    1 -> {
                        if (packageName in enabledAppSetCached) {
                            when (packageName) {
                                "com.whatsapp" -> if (sortKey?.toInt() == 1)
                                    readTTS(textToRead)

                                "com.instagram.android" -> if (key.contains("|direct|"))
                                    readTTS(textToRead)

                                "org.telegram.messenger" -> readTTS(textToRead)

                                else -> readTTS(textToRead)
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun readTTS(text: String) {
        if (!randomVoiceCached || validRandomVoices.isEmpty()) {
            ttsTrySpeak(text)
            return
        }

        val nextVoice = validRandomVoices
            .filter { it != currentVoice }
            .randomOrNull() ?: currentVoice

        try {
            tts.voice = nextVoice
        } catch (_: Exception) {
            DebugLog.add(this@NotificationListener, "Error setting voice: ${nextVoice?.name}")
            try {
                defaultFallbackVoice?.let { tts.voice = it }
                    ?: tts.setLanguage(Locale.getDefault())
            } catch (_: Exception) {
                DebugLog.add(this@NotificationListener, "Fallback didn't work, using system default")
            }
        }

        ttsTrySpeak(text)
    }

    private fun ttsTrySpeak(text: String) {
        if (!ttsReady)
            return

        queueCounter.incrementAndGet()
        if (queueCounter.get() > 20) {
            tts.stop()
            queueCounter.set(0)
        }

        audioManager.requestAudioFocus(focusRequest)

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeCached*volumeCached)
        }
        val utteranceId = System.nanoTime().toString()
        val result = tts.speak(text, TextToSpeech.QUEUE_ADD, params, utteranceId)

        if (result == TextToSpeech.ERROR) {
            restartTTS()

            serviceScope.launch {
                delay(3000)
                tts.speak(text, TextToSpeech.QUEUE_ADD, params, utteranceId)
            }
        }
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
                "forceRestartTTS" -> {
                    restartTTS()
                }
            }
        }
    }
}