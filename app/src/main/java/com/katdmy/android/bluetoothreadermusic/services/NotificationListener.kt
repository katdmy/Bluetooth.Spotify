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
import com.katdmy.android.bluetoothreadermusic.util.BTRMDataStore
import com.katdmy.android.bluetoothreadermusic.util.Constants.RANDOM_VOICE
import com.katdmy.android.bluetoothreadermusic.util.Constants.SERVICE_LAST_HEARTBEAT
import com.katdmy.android.bluetoothreadermusic.util.Constants.TTS_MODE
import com.katdmy.android.bluetoothreadermusic.util.Constants.USE_TTS_SF
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
import com.katdmy.android.bluetoothreadermusic.data.enums.AudioFocusMode
import com.katdmy.android.bluetoothreadermusic.data.enums.NotificationPart
import com.katdmy.android.bluetoothreadermusic.data.models.AppVoiceSettings
import com.katdmy.android.bluetoothreadermusic.data.models.NotificationFingerprint
import com.katdmy.android.bluetoothreadermusic.receivers.BtBroadcastReceiver
import com.katdmy.android.bluetoothreadermusic.util.BTConnectionState
import com.katdmy.android.bluetoothreadermusic.util.Constants.APP_VOICE_SETTINGS
import com.katdmy.android.bluetoothreadermusic.util.Constants.GLOBAL_AUDIOFOCUS_MODE
import com.katdmy.android.bluetoothreadermusic.util.Constants.GLOBAL_NOTIFICATION_PARTS
import com.katdmy.android.bluetoothreadermusic.util.Constants.TTS_VOLUME
import com.katdmy.android.bluetoothreadermusic.util.DebugLog
import com.katdmy.android.bluetoothreadermusic.util.PackageHelper.getAppName
import com.katdmy.android.bluetoothreadermusic.util.ServiceHealthBus
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds


class NotificationListener : NotificationListenerService() {

    private lateinit var btBroadcastReceiver: BtBroadcastReceiver
    private lateinit var listeningCommunicator: ListeningCommunicator
    private lateinit var tts: TextToSpeech
    private lateinit var audioManager: AudioManager
    private lateinit var duckFocusRequest: AudioFocusRequest
    private lateinit var exclusiveFocusRequest: AudioFocusRequest
    private val json = Json { ignoreUnknownKeys = true }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null
    private val ttsMutex = Mutex()
    private var recent = LinkedHashSet<NotificationFingerprint>()
    private val prefs by lazy { getSharedPreferences("service_state", MODE_PRIVATE) }
    private var lastSavedHeartbeat = 0L
    private val queueCounter = AtomicInteger(0)
    private var lastTtsStartTime = 0L
    private val TTS_TIMEOUT = 5 * 60 * 1000L    // 5 минут

    @Volatile
    private var validRandomVoices: List<Voice> = emptyList()
    @Volatile
    private var currentVoiceName: String = ""

    @Volatile
    private var ttsReady: Boolean = false
    @Volatile
    private var useTTSCached: Boolean = true
    @Volatile
    private var ttsModeCached: Int = 0
    @Volatile
    private var randomVoiceCached: Boolean = false
    @Volatile
    private var settingsMapCached: Map<String, AppVoiceSettings> = emptyMap()
    @Volatile
    private var globalAudioFocusMode: AudioFocusMode = AudioFocusMode.DUCK
    @Volatile
    private var globalNotificationParts: Set<NotificationPart> = setOf(
        NotificationPart.TITLE,
        NotificationPart.TEXT
    )
    @Volatile
    private var volumeCached: Float = 1f
    @Volatile
    private var firstSpeakAfterInit = true



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onListenerConnected() {
        DebugLog.add("Service started")

        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                ttsInitialized()
                DebugLog.add("TTS initialized")
                for (engine in tts.engines) {
                    DebugLog.add("Installed engine: ${engine.name}")
                }
                DebugLog.add("Loaded voices: ${validRandomVoices.size}")
            }
            else {
                DebugLog.add("TTS initialization failed")
            }
        }

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        duckFocusRequest =
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).run {
                setAudioAttributes(AudioAttributes.Builder().run {
                    setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    build()
                })
                build()
            }

        exclusiveFocusRequest =
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE).run {
                setAudioAttributes(AudioAttributes.Builder().run {
                    setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    build()
                })
                build()
            }

        btBroadcastReceiver = BtBroadcastReceiver(
            changeConnectionStatus = BTConnectionState::set
        )

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
                BTRMDataStore.getValueFlow(APP_VOICE_SETTINGS, this@NotificationListener)
                    .map { raw ->
                        if (raw == null)
                            emptyList()
                        else
                            try {
                                json.decodeFromString<List<AppVoiceSettings>>(raw)
                            } catch (_: Exception) {
                                emptyList()
                            }
                    }
                    .distinctUntilChanged()
                    .collect { list ->
                        settingsMapCached = list.associateBy { it.packageName }
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

                    delay(10_000L.milliseconds)
                }
            }
            launch {
                BTRMDataStore.getValueFlow(
                    GLOBAL_AUDIOFOCUS_MODE,
                    this@NotificationListener
                )
                    .map { raw ->
                        try {
                            raw?.let {
                                json.decodeFromString<AudioFocusMode>(it)
                            }
                        } catch (_: Exception) {
                            null
                        } ?: AudioFocusMode.DUCK
                    }
                    .collect { globalAudioFocusMode = it }
            }
            launch {
                BTRMDataStore.getValueFlow(
                    GLOBAL_NOTIFICATION_PARTS,
                    this@NotificationListener
                )
                    .map { raw ->
                        try {
                            raw?.let {
                                json.decodeFromString<Set<NotificationPart>>(it)
                            }
                        } catch (_: Exception) {
                            null
                        } ?: setOf(
                            NotificationPart.TITLE,
                            NotificationPart.TEXT
                        )
                    }
                    .collect { globalNotificationParts = it }
            }
        }

        val notificationsIntentFilter = IntentFilter().apply {
            addAction("com.katdmy.android.bluetoothreadermusic.onNotificationStopTTSClick")
            addAction("com.katdmy.android.bluetoothreadermusic.onNotificationStartTTSClick")
            addAction("com.katdmy.android.bluetoothreadermusic.forceRestartTTS")
        }
        val btStatusIntentFilter = IntentFilter().apply {
            addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
        }
        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(listeningCommunicator, notificationsIntentFilter, RECEIVER_EXPORTED)
            registerReceiver(btBroadcastReceiver, btStatusIntentFilter,  RECEIVER_EXPORTED)
        } else {
            registerReceiver(listeningCommunicator, notificationsIntentFilter)
            registerReceiver(btBroadcastReceiver, btStatusIntentFilter)
        }

        startHeartbeat()
    }

    override fun onListenerDisconnected() {
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

                    delay(10_000.milliseconds)
                }
            } catch (_: Throwable) {}
        }
    }

    private fun getFocusRequest(mode: AudioFocusMode): AudioFocusRequest =
        when(mode) {
            AudioFocusMode.DUCK -> duckFocusRequest
            AudioFocusMode.EXCLUSIVE -> exclusiveFocusRequest
        }

    private fun ttsInitialized() {
        audioManager.abandonAudioFocusRequest(duckFocusRequest)
        audioManager.abandonAudioFocusRequest(exclusiveFocusRequest)
        queueCounter.set(0)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                lastTtsStartTime = System.currentTimeMillis()
            }

            override fun onDone(utteranceId: String) {
                lastTtsStartTime = System.currentTimeMillis()
                audioManager.abandonAudioFocusRequest(duckFocusRequest)
                audioManager.abandonAudioFocusRequest(exclusiveFocusRequest)
                queueCounter.updateAndGet {
                    maxOf(0, it - 1)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                audioManager.abandonAudioFocusRequest(duckFocusRequest)
                audioManager.abandonAudioFocusRequest(exclusiveFocusRequest)
                queueCounter.updateAndGet {
                    maxOf(0, it - 1)
                }
            }
        })

        firstSpeakAfterInit = true
        refreshValidVoices()
    }

    override fun onDestroy() {
        DebugLog.add("Service stopped")
        try {
            unregisterReceiver(listeningCommunicator)
        } catch (_: Exception) {}
        try {
            unregisterReceiver(btBroadcastReceiver)
        } catch (_: Exception) {}

        heartbeatJob?.cancel()

        serviceScope.cancel()

        try{
            tts.stop()
            tts.setOnUtteranceProgressListener(null)
            tts.shutdown()
        } catch (_: Exception) {}

        super.onDestroy()
    }

    fun switchTTS(newUseTTS: Boolean) {
        serviceScope.launch {
            ttsMutex.withLock {
                if (!newUseTTS) {
                    try {
                        tts.stop()
                    } catch (_: Exception) {}
                } else {
                    restartTTSInternal()
                }
            }
        }
    }

    private fun restartTTSInternal() {
        try {
            ttsReady = false

            tts.stop()
            tts.setOnUtteranceProgressListener(null)
            tts.shutdown()
        } catch (_: Exception) {}

        tts = TextToSpeech(this@NotificationListener) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) ttsInitialized()
        }
    }

    private fun restartTTS() {
        serviceScope.launch {
            ttsMutex.withLock {
                restartTTSInternal()
            }
        }
    }

    private fun refreshValidVoices() {
        val targetLang = Locale.getDefault().language
        val allVoices = tts.voices ?: run {
            validRandomVoices = emptyList()
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

        if (validRandomVoices.isNotEmpty()) {
            val firstVoice = validRandomVoices.random()

            try {
                tts.voice = firstVoice
                currentVoiceName = firstVoice.name
            } catch (_: Exception) {}
        }
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

        val title = pSender.ifBlank { aTitle }.toString()
        val text = pText.ifBlank { aText }.toString()

        DebugLog.add(
            "pkg=$packageName " +
            "key=$key " +
            "title=$title " +
            "text=$text"
        )

        if (!useTTSCached || key == null)
            return

        when (ttsModeCached) {
            0 -> {
                if (packageName != applicationContext.packageName) {
                    val textToRead = getTextToRead(
                        globalNotificationParts,
                        getAppName(this, packageName) ?: packageName,
                        title,
                        text
                    )

                    if (textToRead.isBlank()) return

                    val fingerprint = NotificationFingerprint(key, "$title|$text")
                    if (!recent.contains(fingerprint)) {
                        recent.add(fingerprint)
                        if (recent.size > 50) {
                            recent.remove(recent.first())
                        }
                    } else return

                    readTTS(textToRead, globalAudioFocusMode)
                }
            }

            1 -> {
                if (settingsMapCached.containsKey(packageName)) {
                    val textToRead = getTextToRead(
                        settingsMapCached[packageName]?.enabledParts ?: globalNotificationParts,
                        getAppName(this, packageName) ?: packageName,
                        title,
                        text
                    )
                    if (textToRead.isBlank()) return

                    val fingerprint = NotificationFingerprint(key, "$title|$text")
                    if (!recent.contains(fingerprint)) {
                        recent.add(fingerprint)
                        if (recent.size > 50) {
                            recent.remove(recent.first())
                        }
                    } else return

                    val audioFocusMode = settingsMapCached[packageName]?.audioFocusMode

                    when (packageName) {
                        "com.whatsapp" -> if (sortKey?.toInt() == 1) {
                            readTTS(textToRead, audioFocusMode)
                        }
                        "com.instagram.android" -> if (key.contains("|direct|"))
                            readTTS(textToRead, audioFocusMode)

                        "org.telegram.messenger" -> readTTS(textToRead, audioFocusMode)

                        else -> readTTS(textToRead, audioFocusMode)
                    }
                }
            }

            else -> {}
        }
    }


    private fun getTextToRead(
        parts: Set<NotificationPart>?,
        app: String,
        title: String,
        text: String
    ): String {
        if (parts == null)
            return ""

        val chunks = mutableListOf<String>()
        if (NotificationPart.APP in parts)
            chunks += app
        if (NotificationPart.TITLE in parts)
            chunks += title
        if (NotificationPart.TEXT in parts)
            chunks += text

        return chunks.joinToString(". ")
    }

    private fun readTTS(text: String, audioFocusMode: AudioFocusMode?) {
        if (!randomVoiceCached || validRandomVoices.isEmpty()) {
            ttsTrySpeak(text, audioFocusMode)
            return
        }

        val nextVoice = validRandomVoices
            .filter { it.name != currentVoiceName }
            .randomOrNull()

        if (nextVoice == null) {
            tts.language = Locale.getDefault()
            DebugLog.add("Error changing voice, using default voice instead. Try check available voices list on Settings screen")
        } else {
            try {
                tts.voice = nextVoice
                currentVoiceName = nextVoice.name
                DebugLog.add("Voice selected: ${nextVoice.name}")
            } catch (_: Exception) {
                tts.language = Locale.getDefault()
                DebugLog.add("Error setting voice: ${nextVoice.name}")
            }
        }

        DebugLog.add("Requested voice = ${nextVoice?.name}")
        DebugLog.add("Actual voice = ${tts.voice?.name}")

        ttsTrySpeak(text, audioFocusMode)
    }

    private fun ttsTrySpeak(text: String, audioFocusMode: AudioFocusMode?) {
        serviceScope.launch {
            ttsMutex.withLock {
                if (!ttsReady)
                    return@withLock

                val focusRequest = getFocusRequest(
                    audioFocusMode ?: globalAudioFocusMode
                )
                audioManager.requestAudioFocus(focusRequest)

                delay(200.milliseconds)

                if (firstSpeakAfterInit) {
                    DebugLog.add("FIRST SPEAK voice=${tts.voice?.name}")
                    firstSpeakAfterInit = false
                }

                val params = Bundle().apply {
                    putFloat(
                        TextToSpeech.Engine.KEY_PARAM_VOLUME,
                        volumeCached*volumeCached
                    )
                }
                val utteranceId = System.nanoTime().toString()
                val result = try {
                    tts.speak(
                        text,
                        TextToSpeech.QUEUE_ADD,
                        params,
                        utteranceId
                    )
                } catch (_: Exception) {
                    TextToSpeech.ERROR
                }

                when (result) {
                    TextToSpeech.ERROR -> {
                        DebugLog.add("TextToSpeech engine error, restarting it and skipping notification.")
                        restartTTSInternal()
                    }

                    TextToSpeech.SUCCESS -> {
                        queueCounter.incrementAndGet()

                        if (queueCounter.get() > 20) {
                            try {
                                tts.stop()
                            } catch (_: Exception) {}

                            queueCounter.set(0)
                        }
                    }
                }
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