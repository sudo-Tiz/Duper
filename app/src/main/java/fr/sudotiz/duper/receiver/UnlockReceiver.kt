package fr.sudotiz.duper.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import fr.sudotiz.duper.service.AlertService
import fr.sudotiz.duper.service.LocationService

class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            Log.d(TAG, "Device unlocked, stopping services")

            context.startService(
                Intent(context, AlertService::class.java).apply {
                    action = AlertService.ACTION_STOP_RING
                }
            )
            context.startService(
                Intent(context, LocationService::class.java).apply {
                    action = LocationService.ACTION_STOP_LOCATE
                }
            )
        }
    }

    companion object {
        private const val TAG = "UnlockReceiver"
    }
}
