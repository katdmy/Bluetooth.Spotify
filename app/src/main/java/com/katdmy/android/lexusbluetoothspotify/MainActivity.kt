package com.katdmy.android.lexusbluetoothspotify

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    //private val bt = BT(applicationContext)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.start_service_btn)?.apply {
            setOnClickListener {
                val startServiceIntent = Intent(context, NotificationListener::class.java)
                context.startForegroundService(startServiceIntent)
            }
        }

        findViewById<Button>(R.id.stop_service_btn)?.apply {
            setOnClickListener {
                val intent = Intent(context, NotificationListener::class.java)
                context.stopService(intent)
            }
        }
    }
}