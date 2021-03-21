package com.katdmy.android.lexusbluetoothspotify

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class ForegroundNotificationListener: Service() {

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService() : ForegroundNotificationListener {
            return this@ForegroundNotificationListener
        }
    }

    fun startNotificationListener() {
        val startIntent = Intent(this, NotificationListener::class.java)
        startService(startIntent)
    }
}