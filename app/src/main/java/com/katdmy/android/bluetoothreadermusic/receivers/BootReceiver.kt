package com.katdmy.android.bluetoothreadermusic.receivers

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.katdmy.android.bluetoothreadermusic.data.ServiceStatus
import com.katdmy.android.bluetoothreadermusic.services.NotificationListener
import com.katdmy.android.bluetoothreadermusic.services.StatusService
import com.katdmy.android.bluetoothreadermusic.util.BluetoothConnectionChecker
import com.katdmy.android.bluetoothreadermusic.worker.RestartServiceWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            //val appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
            //DebugLog.add(context, "BT Reader started, version $appVersion, Android ${Build.VERSION.RELEASE}")

            val serviceIntent = Intent(context, StatusService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)

            BluetoothConnectionChecker(context) { state1 ->
                if (state1 == "DISCONNECTED") {
                    Handler(Looper.getMainLooper()).postDelayed({
                        BluetoothConnectionChecker(context) { //state2 ->
                            //DebugLog.add(context, "Bluetooth state after boot: $state2")
                        }
                    }, 10_000L)
                } else {
                    //DebugLog.add(context, "Bluetooth state after boot: $state1")
                }
            }

            val request = OneTimeWorkRequestBuilder<RestartServiceWorker>()
                .setInitialDelay(10, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "restart_service_after_boot",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }
    }
}