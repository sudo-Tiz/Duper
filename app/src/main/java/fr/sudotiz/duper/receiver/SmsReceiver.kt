package fr.sudotiz.duper.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import fr.sudotiz.duper.DuperApplication
import fr.sudotiz.duper.R
import fr.sudotiz.duper.service.AlertService
import fr.sudotiz.duper.service.LocationService
import fr.sudotiz.duper.util.SmsUtil

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = (context.applicationContext as DuperApplication).preferencesRepository
        val prefix = prefs.commandPrefix
        val code = prefs.commandCode
        val suffix = if (code.isBlank()) "" else " ${code.trim()}"
        val ringEnabled = prefs.ringEnabled
        val locateEnabled = prefs.locateEnabled

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        for (smsMessage in messages) {
            val messageBody = smsMessage.messageBody.trim()
            val sender = smsMessage.originatingAddress ?: continue

            val messageLower = messageBody.lowercase()
            val ringCommand = "$prefix ring$suffix".lowercase()
            val locateCommand = "$prefix locate$suffix".lowercase()

            if (messageLower == ringCommand && ringEnabled) {
                Log.d(TAG, "Ring command detected! Starting alert...")
                SmsUtil.send(context, sender, context.getString(R.string.sms_ring_activated))
                prefs.recordCommand("ring", sender)

                val alertIntent = Intent(context, AlertService::class.java).apply {
                    action = AlertService.ACTION_START_RING
                }
                startService(context, alertIntent)
            }

            if (messageLower == locateCommand && locateEnabled) {
                Log.d(TAG, "Locate command detected! Starting location tracking...")
                SmsUtil.send(context, sender, context.getString(R.string.sms_locate_received))
                prefs.recordCommand("locate", sender)

                val locationIntent = Intent(context, LocationService::class.java).apply {
                    action = LocationService.ACTION_START_LOCATE
                    putExtra(LocationService.EXTRA_SENDER, sender)
                }
                startService(context, locationIntent)
            }
        }
    }

    private fun startService(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
