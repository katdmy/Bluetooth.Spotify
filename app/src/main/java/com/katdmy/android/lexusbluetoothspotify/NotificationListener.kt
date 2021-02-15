package com.katdmy.android.lexusbluetoothspotify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast

class NotificationListener : NotificationListenerService() {

    private var isConnected = false
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "ChannelId"


    override fun onCreate() {
        super.onCreate()
        val notification = createNotification(
                this,
                getString(R.string.service_name)
        )
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (isConnected) {
            /*Toast.makeText(applicationContext, sbn.toString(), Toast.LENGTH_LONG).show()
            Log.e(NotificationListener::class.java.simpleName, "Notification posted: ${sbn.toString()}")*/
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
    }

    private fun createNotification(
            context: Context,
            title: String,
            pendingIntent: PendingIntent? = null
    ): Notification {
        val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(createChannel(context))

        return createNotification(context, title, pendingIntent)
    }

    private fun createChannel(context: Context): NotificationChannel {
        return NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.service_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.service_channel_description)
            setSound(null, null)
            enableVibration(false)
        }
    }

}