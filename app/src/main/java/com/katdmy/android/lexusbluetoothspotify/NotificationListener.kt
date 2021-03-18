package com.katdmy.android.lexusbluetoothspotify

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationListener : NotificationListenerService() {

    private val TAG = this.javaClass.simpleName


    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        //Log.e(TAG, "********** onNotificationPosted")
        //Log.e(TAG, "ID : ${sbn?.id} \t ${sbn?.notification?.tickerText} \t ${sbn?.packageName} \t ${sbn?.notification?.extras}")
        val intent = Intent("com.katdmy.android.lexusbluetoothspotify")
        intent.putExtra("Package Name", sbn?.packageName)
        intent.putExtra("Key", sbn?.key)
        intent.putExtra("SBN", sbn?.toString())
        sendBroadcast(intent)

        if (sbn?.packageName == "ru.alarmtrade.connect"
                && sbn.key == "0|ru.alarmtrade.connect|1076889714|null|10269") {
            val pandoraIntent = Intent("com.katdmy.android.lexusbluetoothspotify.pandora")
            sendBroadcast(pandoraIntent)
        }

        /*if (sbn?.packageName == "com.katdmy.android.lexusbluetoothspotify")
                //&& sbn.key == "0|com.katdmy.android.lexusbluetoothspotify|624949549|null|10332")
            sendBroadcast(intent)*/
    }
}