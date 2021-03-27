package com.katdmy.android.lexusbluetoothspotify

import android.app.*
import android.content.*
import android.graphics.Color
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.preference.PreferenceManager


class NotificationListener : NotificationListenerService() {

    private val TAG = this.javaClass.simpleName
    private val FOREGROUND_NOTIFICATION_CHANNEL_ID = 10001
    private var useTTS = false
    private var oldSbn: StatusBarNotification? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var listeningCommunicator: ListeningCommunicator
    private lateinit var tts: TextToSpeech

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "$TAG is started with intent: $intent")
        tts = TextToSpeech(this, null)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        useTTS = sharedPreferences.getBoolean(BtNames.useTTS_SF, false)
        listeningCommunicator = ListeningCommunicator(useTTS, sharedPreferences)

        return START_STICKY
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val intent = Intent("com.katdmy.android.lexusbluetoothspotify")
        intent.putExtra("Package Name", sbn?.packageName)
        intent.putExtra("Key", sbn?.key)
        intent.putExtra("Title", sbn?.notification?.extras?.getString("android.title"))
        intent.putExtra("Text", sbn?.notification?.extras?.getString("android.text"))
        if (sbn != oldSbn) {
            sendBroadcast(intent)
            oldSbn = sbn
        }

        /*if (sbn?.packageName == "ru.alarmtrade.connect"
            && sbn.key == "0|ru.alarmtrade.connect|1076889714|null|10269") {
            val pandoraIntent = Intent("com.katdmy.android.lexusbluetoothspotify.pandora")
            sendBroadcast(pandoraIntent)
        }*/

        if (useTTS) {
            val key = sbn?.key
            if ((key?.contains("0|com.whatsapp|1") == true && !sbn.key?.contains("0|com.whatsapp|1|null")!!)
                    || packageName == "org.telegram.messenger") {
                val title = intent.getStringExtra("Title") ?: ""
                val text = intent.getStringExtra("Text") ?: ""
                val data = "$title - $text"
                tts.speak(data, TextToSpeech.QUEUE_ADD, null, data)
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(listeningCommunicator)
        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.e(TAG, "Service Reader Connected")
        val not = createNotification()
        val mNotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(FOREGROUND_NOTIFICATION_CHANNEL_ID, not)

        startForeground(FOREGROUND_NOTIFICATION_CHANNEL_ID, not)

        val notificationsIntentFilter = IntentFilter().apply {
            addAction("com.katdmy.android.lexusbluetoothspotify.onVoiceUseChange")
        }
        registerReceiver(listeningCommunicator, notificationsIntentFilter)

    }

    private fun createNotification(): Notification {

        val channelId = createNotificationChannel("my_service", "My Background Service")

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        return Notification.Builder(this, channelId)
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getText(R.string.notification_message))
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentIntent(pendingIntent)
            //.setTicker(getText(R.string.ticker_text))
            .build()
    }

    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    class ListeningCommunicator(
            private var useTTS: Boolean,
            private val sharedPreferences: SharedPreferences
            ) : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            Log.d(this.javaClass.simpleName, "***** onVoiceUseChange: Received command")
            if (intent.getStringExtra("command") == "onVoiceUseChange") {
                useTTS = sharedPreferences.getBoolean(BtNames.useTTS_SF, false)
                Log.d(this.javaClass.simpleName, "got from sharedPreferences: useTTS = $useTTS")
            }
        }
    }

}