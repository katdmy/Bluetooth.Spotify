package com.katdmy.android.lexusbluetoothspotify

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationListener : NotificationListenerService() {

    private val TAG = this.javaClass.simpleName

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        Log.e(TAG, "********** onNotificationPosted")
        Log.e(TAG, "ID : ${sbn?.id} \t ${sbn?.notification?.tickerText}  \t ${sbn?.packageName}")
        val intent = Intent("com.katdmy.android.lexusbluetoothspotify")
        intent.putExtra("Package Name", sbn?.packageName)
        intent.putExtra("Ticker Text", sbn?.notification?.tickerText)
        intent.putExtra("Key", sbn?.key)
        intent.putExtra("Title", sbn?.notification?.extras?.getString("EXTRA_TITLE"))
        intent.putExtra("Text", sbn?.notification?.extras?.getCharSequence("EXTRA_TEXT").toString())
        intent.putExtra("Notification", sbn?.notification?.toString())
        intent.putExtra("SBN", sbn?.toString())
        sendBroadcast(intent)
    }
}