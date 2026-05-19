package com.katdmy.android.bluetoothreadermusic.worker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.katdmy.android.bluetoothreadermusic.data.ServiceStatus
import com.katdmy.android.bluetoothreadermusic.services.NotificationListener
import com.katdmy.android.bluetoothreadermusic.services.StatusService
import kotlinx.coroutines.delay

class RestartServiceWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!isNotificationListenerEnabled())
            return Result.success()

        restartNotificationListener()

        val serviceIntent = Intent(applicationContext, StatusService::class.java)

        delay(5000)

        if (StatusService.serviceHealth.value != ServiceStatus.Working) {
            ContextCompat.startForegroundService(
                applicationContext,
                serviceIntent
            )
        }

        return Result.success()
    }

    private fun isNotificationListenerEnabled(): Boolean {

        val cn = ComponentName(
            applicationContext,
            NotificationListener::class.java
        )

        val flat = Settings.Secure.getString(
            applicationContext.contentResolver,
            "enabled_notification_listeners"
        )

        return flat?.contains(cn.flattenToString()) == true
    }

    private fun restartNotificationListener() {
        val cn = ComponentName(applicationContext, NotificationListener::class.java)
        val pm = applicationContext.packageManager

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