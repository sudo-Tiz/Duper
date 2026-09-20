package fr.sudotiz.duper.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import fr.sudotiz.duper.data.CommandType
import fr.sudotiz.duper.DuperApplication
import fr.sudotiz.duper.R
import fr.sudotiz.duper.service.AlertService
import fr.sudotiz.duper.service.LocationService
import fr.sudotiz.duper.util.CommandNotification
import fr.sudotiz.duper.util.SmsUtil

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = (context.applicationContext as DuperApplication).preferencesRepository
        val prefix = prefs.commandPrefix
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        for (smsMessage in messages) {
            val messageBody = smsMessage.messageBody.trim()
            val sender = smsMessage.originatingAddress ?: continue

            when (val command = resolveCommand(messageBody, prefix, prefs.ringPassword, prefs.locateSecret)) {
                is Command.Ring -> if (!prefs.ringEnabled) {
                    notify(context, sender, R.string.command_ring, R.string.command_refused_disabled)
                } else if (!command.passwordValid) {
                    notify(context, sender, R.string.command_ring, R.string.command_refused_password)
                } else {
                    Log.d(TAG, "Ring command detected! Starting alert...")
                    val replied = SmsUtil.send(context, sender, context.getString(R.string.sms_ring_activated))
                    prefs.recordCommand(CommandType.RING, sender)

                    val alertIntent = Intent(context, AlertService::class.java).apply {
                        action = AlertService.ACTION_START_RING
                    }
                    startService(context, alertIntent)
                    notify(
                        context, sender, R.string.command_ring,
                        if (replied) R.string.command_reply_sent else R.string.command_reply_not_sent
                    )
                }

                is Command.Locate -> if (!prefs.locateEnabled) {
                    notify(context, sender, R.string.command_locate, R.string.command_refused_disabled)
                } else if (!command.secretValid) {
                    notify(context, sender, R.string.command_locate, R.string.command_refused_secret)
                } else {
                    Log.d(TAG, "Locate command detected! Starting location tracking...")
                    val replied = SmsUtil.send(context, sender, context.getString(R.string.sms_locate_received))
                    prefs.recordCommand(CommandType.LOCATE, sender)

                    val locationIntent = Intent(context, LocationService::class.java).apply {
                        action = LocationService.ACTION_START_LOCATE
                        putExtra(LocationService.EXTRA_SENDER, sender)
                    }
                    startService(context, locationIntent)
                    notify(
                        context, sender, R.string.command_locate,
                        if (replied) R.string.command_reply_sent else R.string.command_reply_not_sent
                    )
                }

                null -> Unit
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

    private fun notify(context: Context, sender: String, command: Int, result: Int) {
        CommandNotification.show(
            context,
            context.getString(R.string.notification_command_title, context.getString(command)),
            context.getString(result, sender)
        )
    }

    private sealed interface Command {
        data class Ring(val passwordValid: Boolean) : Command
        data class Locate(val secretValid: Boolean) : Command
    }

    private fun resolveCommand(
        message: String,
        prefix: String,
        ringPassword: String,
        locateSecret: String,
    ): Command? {
        val input = message.trim().lowercase()
        val normalizedPrefix = prefix.trim().lowercase()
        if (normalizedPrefix.isBlank() || !input.startsWith(normalizedPrefix)) return null
        val parts = input.split(Regex("\\s+"))
        if (parts.firstOrNull() != normalizedPrefix) return null
        val password = ringPassword.trim().lowercase()
        val secret = locateSecret.trim().lowercase()
        return when {
            parts.size == 1 -> Command.Ring(password.isBlank())
            parts[1] == "ring" -> Command.Ring(
                when {
                    password.isBlank() -> parts.size == 2
                    else -> parts.size == 3 && parts[2] == password
                }
            )
            parts[1] == "locate" -> Command.Locate(
                secret.isNotBlank() && parts.size == 3 && parts[2] == secret
            )
            password.isNotBlank() && parts.size == 2 -> Command.Ring(parts[1] == password)
            else -> null
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
