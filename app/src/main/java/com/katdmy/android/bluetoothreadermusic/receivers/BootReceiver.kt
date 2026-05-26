package com.katdmy.android.bluetoothreadermusic.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.katdmy.android.bluetoothreadermusic.services.StatusService
import com.katdmy.android.bluetoothreadermusic.util.BluetoothConnectionChecker
import com.katdmy.android.bluetoothreadermusic.util.DebugLog
import com.katdmy.android.bluetoothreadermusic.worker.RestartServiceWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
            DebugLog.add("BT Reader started, version $appVersion, Android ${Build.VERSION.RELEASE}")

            val serviceIntent = Intent(context, StatusService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)

            BluetoothConnectionChecker(context) { state ->
                DebugLog.add("Bluetooth state after boot: $state")
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