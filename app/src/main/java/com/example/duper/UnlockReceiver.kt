package com.example.duper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class UnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            Log.d("UnlockReceiver", "Device unlocked, stopping services")

            val alertIntent = Intent(context, AlertService::class.java).apply {
                action = "STOP_RING"
            }
            context.startService(alertIntent)

            val locationIntent = Intent(context, LocationService::class.java).apply {
                action = "STOP_LOCATE"
            }
            context.startService(locationIntent)
        }
    }
}
