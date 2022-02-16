package com.katdmy.android.bluetoothspotify

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.preference.PreferenceManager


class NotificationListener : NotificationListenerService() {

    private val TAG = this.javaClass.simpleName
    private val FOREGROUND_NOTIFICATION_ID = 10001
    private var useTTS = false
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var listeningCommunicator: ListeningCommunicator
    private lateinit var tts: TextToSpeech
    private lateinit var audioManager: AudioManager
    private lateinit var focusRequest: AudioFocusRequest
    private val btBroadcastReceiver =
        BtBroadcastReceiver { status -> switchTTS(applicationContext, status) }
    private var errorMessage = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "onStartCommand(): intent = $intent")

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
        useTTS = sharedPreferences.getBoolean(BtNames.useTTS_SF, false)
        listeningCommunicator = ListeningCommunicator()

        createNotification()

        val notificationsIntentFilter = IntentFilter().apply {
            addAction("com.katdmy.android.bluetoothspotify.notificationListenerServiceTTS")
            addAction("com.katdmy.android.bluetoothspotify.notificationListenerService")
            addAction("com.katdmy.android.bluetoothspotify.showNotificationWithError")
        }
        registerReceiver(listeningCommunicator, notificationsIntentFilter)

        val btStatusIntentFilter = IntentFilter().apply {
            addAction("android.bluetooth.device.action.ACL_CONNECTED")
            addAction("android.bluetooth.device.action.ACL_DISCONNECTED")
        }
        registerReceiver(btBroadcastReceiver, btStatusIntentFilter)
    }

    private fun ttsInitialized() {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                audioManager.requestAudioFocus(focusRequest)
            }

            override fun onDone(utteranceId: String?) {
                audioManager.abandonAudioFocusRequest(focusRequest)
            }

            override fun onError(utteranceId: String?) {
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

        val editor = sharedPreferences.edit()
        editor.putBoolean(BtNames.useTTS_SF, useTTS)
        editor.apply()

        val switchTTSIntent = Intent("com.katdmy.android.bluetoothspotify")
        if (useTTS) switchTTSIntent.putExtra("command", "onNotificationStartTTSClick")
        else switchTTSIntent.putExtra("command", "onNotificationStopTTSClick")
        context.sendBroadcast(switchTTSIntent)

        createNotification()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val intent = Intent("com.katdmy.android.bluetoothspotify")
        intent.putExtra("Package Name", sbn?.packageName)
        intent.putExtra("Key", sbn?.key)
        intent.putExtra("Title", sbn?.notification?.extras?.getString("android.title"))
        intent.putExtra("Text", sbn?.notification?.extras?.getString("android.text"))
        sendBroadcast(intent)

        if (useTTS) {
            val key = sbn?.key
            if ((key?.contains("0|com.whatsapp|1") == true && !sbn.key?.contains("0|com.whatsapp|1|null")!!)
                    || packageName == "org.telegram.messenger") {
                val title = intent.getStringExtra("Title") ?: ""
                val text = intent.getStringExtra("Text") ?: ""
                val data = "$title - $text"
                if (sbn?.notification?.sortKey == "1") {
                    tts.speak(data, TextToSpeech.QUEUE_ADD, null, data)
                }
            }
        }
    }

    fun createNotification() {
        val channelId = createNotificationChannel("my_service", "My Background Service")

        val openActivityPendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { openActivityIntent ->
                PendingIntent.getActivity(this, 0, openActivityIntent, PendingIntent.FLAG_IMMUTABLE)
            }

        val switchTTSIntent =
            Intent("com.katdmy.android.bluetoothspotify.notificationListenerServiceTTS").apply {
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
            Intent("com.katdmy.android.bluetoothspotify.notificationListenerService").apply {
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

        stopForeground(false)
        startForeground(FOREGROUND_NOTIFICATION_ID, foregroundNotification)
    }

    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }


    inner class ListeningCommunicator : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            Log.e(TAG, "command received:  ${intent.getStringExtra("command")}")
            if (intent.getStringExtra("command") == "onVoiceUseChange") {
                useTTS = sharedPreferences.getBoolean(BtNames.useTTS_SF, false)
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
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
                )
            }
            if (intent.getStringExtra("command") == "showNotificationWithError") {
                errorMessage = intent.getStringExtra("errorMessage") ?: ""
                createNotification()
            }
        }
    }

}