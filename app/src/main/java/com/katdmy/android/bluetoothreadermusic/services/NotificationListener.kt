package com.katdmy.android.bluetoothreadermusic.services

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothProfile
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
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
import androidx.preference.PreferenceManager
import com.katdmy.android.bluetoothreadermusic.Constants.USE_TTS_SF
import com.katdmy.android.bluetoothreadermusic.Constants.MUSIC_PACKAGE_NAME
import com.katdmy.android.bluetoothreadermusic.presentation.MainActivity
import com.katdmy.android.bluetoothreadermusic.R


class NotificationListener : NotificationListenerService() {

    private val TAG = this.javaClass.simpleName
    private val FOREGROUND_NOTIFICATION_ID = 10001
    private var useTTS = false
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var listeningCommunicator: ListeningCommunicator
    private lateinit var tts: TextToSpeech
    private lateinit var audioManager: AudioManager
    private lateinit var focusRequest: AudioFocusRequest
    private var lastReadData = ""
    private var errorMessage = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onListenerConnected() {
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
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        useTTS = sharedPreferences.getBoolean(USE_TTS_SF, false)
        listeningCommunicator = ListeningCommunicator()

        createNotification()

        val notificationsIntentFilter = IntentFilter().apply {
            addAction("com.katdmy.android.bluetoothreadermusic.notificationListenerServiceTTS")
            addAction("com.katdmy.android.bluetoothreadermusic.notificationListenerService")
            addAction("com.katdmy.android.bluetoothreadermusic.showNotificationWithError")
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
        unregisterReceiver(listeningCommunicator)
        super.onDestroy()
    }


    fun switchTTS(context: Context, status: Boolean) {
        useTTS = status
        if (!useTTS) tts.stop()

        val spEditor = sharedPreferences.edit()
        spEditor.putBoolean(USE_TTS_SF, useTTS).apply()

        val switchTTSIntent = Intent("com.katdmy.android.bluetoothreadermusic")
        if (useTTS) switchTTSIntent.putExtra("command", "onNotificationStartTTSClick")
        else switchTTSIntent.putExtra("command", "onNotificationStopTTSClick")
        context.sendBroadcast(switchTTSIntent)

        createNotification()
        if (status)
            openMusic()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val intent = Intent("com.katdmy.android.bluetoothreadermusic")
        intent.putExtra("Package Name", sbn?.packageName)
        intent.putExtra("Key", sbn?.key)
        intent.putExtra("Title", sbn?.notification?.extras?.getCharSequence("android.title"))
        intent.putExtra("Text", sbn?.notification?.extras?.getCharSequence("android.text"))
        sendBroadcast(intent)

        if (useTTS) {
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
                }
            }
        }
    }

    fun createNotification() {
        val channelId = createNotificationChannel()

        val openActivityPendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { openActivityIntent ->
                PendingIntent.getActivity(this, 0, openActivityIntent, PendingIntent.FLAG_IMMUTABLE)
            }

        val switchTTSIntent =
            Intent("com.katdmy.android.bluetoothreadermusic.notificationListenerServiceTTS").apply {
                putExtra("command", "onNotificationSwitchTTSClick")
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
            Intent("com.katdmy.android.bluetoothreadermusic.notificationListenerService").apply {
                putExtra("command", "stopServiceIntentClick")
            }
        val stopServicePendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, stopServiceIntent, PendingIntent.FLAG_IMMUTABLE)
        val stopServiceAction: Notification.Action = Notification.Action.Builder(
            null,
            getString(R.string.stopService),
            stopServicePendingIntent
        )
            .build()

        val icon = if (useTTS) R.drawable.ic_notifications
        else R.drawable.ic_outline_notifications

        val foregroundNotification = Notification.Builder(this, channelId)
            .setContentTitle(getText(R.string.notification_title))
            //.setContentText(getText(R.string.notification_message))
            .setStyle(Notification.BigTextStyle().bigText(errorMessage))
            .setSmallIcon(icon)
            .setContentIntent(openActivityPendingIntent)
            .addAction(switchTTSAction)
            .addAction(stopServiceAction)
            .build()

        val mNotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(FOREGROUND_NOTIFICATION_ID, foregroundNotification)

        stopForeground(STOP_FOREGROUND_REMOVE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(FOREGROUND_NOTIFICATION_ID, foregroundNotification, FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING)
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, foregroundNotification)
        }
    }

    private fun createNotificationChannel(): String {
        val chan = NotificationChannel(
            "my_service",
            "My Background Service", NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return "my_service"
    }

    private fun openMusic() {
        val selectedMusicApp = sharedPreferences.getString(MUSIC_PACKAGE_NAME, "") ?: ""
        val launchIntent = packageManager.getLaunchIntentForPackage(selectedMusicApp)
        if (launchIntent != null)
            startActivity(launchIntent)
    }


    inner class ListeningCommunicator : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val command = intent.getStringExtra("command")
            if (command != null) {
                Log.e(TAG, "command received:  $command")
                if (intent.getStringExtra("command") == "onVoiceUseChange") {
                    useTTS = sharedPreferences.getBoolean(USE_TTS_SF, false)
                    createNotification()
                    Log.d(this.javaClass.simpleName, "got from sharedPreferences: useTTS = $useTTS")
                }
                if (intent.getStringExtra("command") == "onNotificationSwitchTTSClick") {
                    useTTS = !useTTS
                    createNotification()
                    switchTTS(context, useTTS)
                }
                if (intent.getStringExtra("command") == "stopServiceIntentClick") {
                    val pm = context.packageManager
                    pm.setComponentEnabledSetting(
                        ComponentName(context, NotificationListener::class.java),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
                if (intent.getStringExtra("command") == "showNotificationWithError") {
                    errorMessage = intent.getStringExtra("errorMessage") ?: ""
                    createNotification()
                }
            } else {
                when (intent.extras?.getInt(BluetoothProfile.EXTRA_STATE)) {
                    BluetoothProfile.STATE_DISCONNECTED -> switchTTS(context, false)
                    BluetoothProfile.STATE_CONNECTED -> switchTTS(context, true)
                }
            }
        }
    }

}