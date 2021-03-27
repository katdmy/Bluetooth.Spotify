package com.katdmy.android.lexusbluetoothspotify

import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.preference.PreferenceManager


class NotificationListener : NotificationListenerService() {

    private val TAG = this.javaClass.simpleName
    private val FOREGROUND_NOTIFICATION_CHANNEL_ID = 10001
    private var useTTS = false
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var listeningCommunicator: ListeningCommunicator
    private lateinit var tts: TextToSpeech


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "onStartCommand(): intent = $intent")

        return START_STICKY
    }

    override fun onListenerConnected() {
        Log.e(TAG, "onListenerConnected(): applicationContext = $applicationContext")
        val foregroundNotification = createNotification()
        val mNotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(FOREGROUND_NOTIFICATION_CHANNEL_ID, foregroundNotification)

        startForeground(FOREGROUND_NOTIFICATION_CHANNEL_ID, foregroundNotification)

        tts = TextToSpeech(this, null)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        useTTS = sharedPreferences.getBoolean(BtNames.useTTS_SF, false)
        listeningCommunicator = ListeningCommunicator(useTTS, sharedPreferences)

        val notificationsIntentFilter = IntentFilter().apply {
            addAction("com.katdmy.android.lexusbluetoothspotify.notificationListenerService")
        }
        registerReceiver(listeningCommunicator, notificationsIntentFilter)
    }

    override fun onDestroy() {
        unregisterReceiver(listeningCommunicator)
        super.onDestroy()
    }


    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val intent = Intent("com.katdmy.android.lexusbluetoothspotify")
        intent.putExtra("Package Name", sbn?.packageName)
        intent.putExtra("Key", sbn?.key)
        intent.putExtra("Title", sbn?.notification?.extras?.getString("android.title"))
        intent.putExtra("Text", sbn?.notification?.extras?.getString("android.text"))
        sendBroadcast(intent)

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

    private fun createNotification(): Notification {
        val channelId = createNotificationChannel("my_service", "My Background Service")

        val openActivityPendingIntent: PendingIntent =
                Intent(this, MainActivity::class.java).let { openActivityIntent ->
                    PendingIntent.getActivity(this, 0, openActivityIntent, 0)
                }

        Intent()

        val stopTTSPendingIntent: PendingIntent =
                Intent("com.katdmy.android.lexusbluetoothspotify.notificationListenerService").let { stopTTSIntent ->
                    stopTTSIntent.putExtra("command", "onNotificationStopTTSClick")
                    PendingIntent.getActivity(this, 0, stopTTSIntent, 0)
                }
        val stopTTSAction: Notification.Action = Notification.Action.Builder(
                Icon.createWithResource(this, R.drawable.ic_mute),
                getString(R.string.stopTTS),
                stopTTSPendingIntent)
                .build()

        val stopServicePendingIntent: PendingIntent =
                Intent("com.katdmy.android.lexusbluetoothspotify.notificationListenerService").let { stopServiceIntent ->
                    stopServiceIntent.putExtra("command", "stopServiceIntentClick")
                    PendingIntent.getActivity(this, 0, stopServiceIntent, 0)
                }
        val stopServiceAction: Notification.Action = Notification.Action.Builder(
                Icon.createWithResource(this, R.drawable.ic_close),
                getString(R.string.stopService),
                stopServicePendingIntent)
                .build()

        return Notification.Builder(this, channelId)
                .setContentTitle(getText(R.string.notification_title))
                .setContentText(getText(R.string.notification_message))
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentIntent(openActivityPendingIntent)
                //.setTicker(getText(R.string.ticker_text))
                .addAction(stopTTSAction)
                .addAction(stopServiceAction)
                //.addAction(, )
                .build()
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
            if (intent.getStringExtra("command") == "onNotificationStopTTSClick") {
                useTTS = false
                val editor = sharedPreferences.edit()
                editor.putBoolean(BtNames.useTTS_SF, false)
                editor.apply()

                val stopTTSIntent = Intent("com.katdmy.android.lexusbluetoothspotify")
                stopTTSIntent.putExtra("command", "onNotificationStopTTSClick")
                context.sendBroadcast(stopTTSIntent)
            }
            if (intent.getStringExtra("command") == "onNotificationStopTTSClick") {
                val pm = context.packageManager
                pm.setComponentEnabledSetting(ComponentName(context, NotificationListener::class.java), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
        }
    }

}