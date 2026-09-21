package fr.sudotiz.duper.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

abstract class DuperForegroundService : Service() {

    protected val handler = Handler(Looper.getMainLooper())
    protected abstract val notifChannelId: String
    protected abstract val notifChannelName: Int
    protected abstract val notifChannelDescription: Int
    protected abstract val notifChannelImportance: Int
    protected abstract val notifId: Int
    protected abstract val notifTitle: Int
    protected abstract val notifText: Int
    protected abstract val notifIcon: Int
    protected abstract val notifPriority: Int

    protected fun buildAndStartForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notifChannelId, getString(notifChannelName), notifChannelImportance
            ).apply { description = getString(notifChannelDescription) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, notifChannelId)
            .setContentTitle(getString(notifTitle))
            .setContentText(getString(notifText))
            .setSmallIcon(notifIcon)
            .setPriority(notifPriority)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notifId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(notifId, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
