package com.katdmy.android.bluetoothreadermusic.receivers

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.katdmy.android.bluetoothreadermusic.data.ServiceStatus
import com.katdmy.android.bluetoothreadermusic.services.NotificationListener
import com.katdmy.android.bluetoothreadermusic.services.StatusService
import com.katdmy.android.bluetoothreadermusic.util.BluetoothConnectionChecker

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (StatusService.serviceHealth.value != ServiceStatus.Working) {

                val cn = ComponentName(context, NotificationListener::class.java)
                val pm = context.packageManager

                pm.setComponentEnabledSetting(
                    cn,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )

                pm.setComponentEnabledSetting(
                    cn,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )

            }

            val serviceIntent = Intent(context, StatusService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)

            BluetoothConnectionChecker(context) {
                if (it == "DISCONNECTED") {
                    Handler(Looper.getMainLooper()).postDelayed({
                        BluetoothConnectionChecker(context) {}
                    }, 10_000L)
                }
            }
        }
    }
}