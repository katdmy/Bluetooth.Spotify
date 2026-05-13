package com.katdmy.android.bluetoothreadermusic.receivers

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.katdmy.android.bluetoothreadermusic.data.ServiceStatus
import com.katdmy.android.bluetoothreadermusic.services.NotificationListener
import com.katdmy.android.bluetoothreadermusic.services.StatusService
import com.katdmy.android.bluetoothreadermusic.util.BluetoothConnectionChecker
import com.katdmy.android.bluetoothreadermusic.util.DebugLog

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
            DebugLog.add(context, "BT Reader started, version $appVersion, Android ${Build.VERSION.RELEASE}")

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
        }

        val serviceIntent = Intent(context, StatusService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)

        BluetoothConnectionChecker(context) { state1 ->
            if (state1 == "DISCONNECTED") {
                Handler(Looper.getMainLooper()).postDelayed({
                    BluetoothConnectionChecker(context) { state2 ->
                        DebugLog.add(context, "Bluetooth state after boot: $state2")
                    }
                }, 10_000L)
            } else {
                DebugLog.add(context, "Bluetooth state after boot: $state1")
            }
        }
    }
}