package com.example.duper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.content.edit

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val prefs = context.getSharedPreferences("duper_prefs", Context.MODE_PRIVATE)
        val prefix = prefs.getString("command_prefix", "duper") ?: "duper"
        val code = prefs.getString("command_code", "") ?: ""
        val suffix = if (code.isBlank()) "" else " ${code.trim()}"
        val ringEnabled = prefs.getBoolean("ring_enabled", true)
        val locateEnabled = prefs.getBoolean("locate_enabled", true)

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        for (smsMessage in messages) {
            val messageBody = smsMessage.messageBody.trim()
            val sender = smsMessage.originatingAddress ?: continue

            val messageLower = messageBody.lowercase()
            val ringCommand = "$prefix ring$suffix".lowercase()
            val locateCommand = "$prefix locate$suffix".lowercase()

            if (messageLower == ringCommand && ringEnabled) {
                Log.d("SmsReceiver", "Ring command detected! Starting alert...")

                SmsUtil.send(context, sender, "Duper: Ring activated, phone is ringing now")
                prefs.edit {
                    putLong("last_command_time", System.currentTimeMillis())
                    putString("last_command_sender", sender)
                    putString("last_command_type", "ring")
                }

                val alertIntent = Intent(context, AlertService::class.java).apply {
                    action = "START_RING"
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(alertIntent)
                } else {
                    context.startService(alertIntent)
                }
            }

            if (messageLower == locateCommand && locateEnabled) {
                Log.d("SmsReceiver", "Locate command detected! Starting location tracking...")

                SmsUtil.send(context, sender, "Duper: Locate command received, working on it")
                prefs.edit {
                    putLong("last_command_time", System.currentTimeMillis())
                    putString("last_command_sender", sender)
                    putString("last_command_type", "locate")
                }

                val locationIntent = Intent(context, LocationService::class.java).apply {
                    action = "START_LOCATE"
                    putExtra("sender", sender)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(locationIntent)
                } else {
                    context.startService(locationIntent)
                }
            }
        }
    }
}