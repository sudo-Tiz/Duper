package fr.sudotiz.duper.receiver

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import fr.sudotiz.duper.DuperApplication
import fr.sudotiz.duper.R
import fr.sudotiz.duper.data.CommandType
import fr.sudotiz.duper.data.PreferencesRepository
import fr.sudotiz.duper.service.AlertService
import fr.sudotiz.duper.service.LocationService
import fr.sudotiz.duper.util.CommandNotification
import fr.sudotiz.duper.util.SmsUtil
import java.util.Locale

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = (context.applicationContext as DuperApplication).preferencesRepository
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        for (smsMessage in messages) {
            val messageBody = smsMessage.messageBody.trim()
            val sender = smsMessage.originatingAddress ?: continue
            val deviceLocked = (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
                .isKeyguardLocked

            when (val result = evaluateCommand(messageBody, prefs, deviceLocked)) {
                CommandResult.Ignored -> Unit
                is CommandResult.Rejected -> notifyRejection(context, sender, result)
                is CommandResult.Accepted -> executeCommand(context, sender, result.type, prefs)
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

    private fun notifyRejection(context: Context, sender: String, result: CommandResult.Rejected) {
        val command = when (result.type) {
            CommandType.RING -> R.string.command_ring
            CommandType.LOCATE -> R.string.command_locate
        }
        val reason = when (result.reason) {
            RejectionReason.DISABLED -> R.string.command_refused_disabled
            RejectionReason.INVALID_PASSWORD -> R.string.command_refused_password
            RejectionReason.INVALID_SECRET -> R.string.command_refused_secret
            RejectionReason.DEVICE_UNLOCKED -> R.string.command_refused_unlocked
        }
        notify(context, sender, command, reason)
    }

    private fun executeCommand(
        context: Context,
        sender: String,
        type: CommandType,
        prefs: PreferencesRepository,
    ) {
        when (type) {
            CommandType.RING -> {
                Log.d(TAG, "Ring command detected! Starting alert...")
                val replied = prefs.ringReplyEnabled &&
                    SmsUtil.send(context, sender, context.getString(R.string.sms_ring_activated))
                prefs.recordCommand(type, sender)
                startService(context, Intent(context, AlertService::class.java).apply {
                    action = AlertService.ACTION_START_RING
                })
                notify(
                    context, sender, R.string.command_ring,
                    when {
                        !prefs.ringReplyEnabled -> R.string.command_reply_disabled
                        replied -> R.string.command_reply_sent
                        else -> R.string.command_reply_not_sent
                    }
                )
            }
            CommandType.LOCATE -> {
                Log.d(TAG, "Locate command detected! Starting location tracking...")
                val replied = SmsUtil.send(context, sender, context.getString(R.string.sms_locate_received))
                prefs.recordCommand(type, sender)
                startService(context, Intent(context, LocationService::class.java).apply {
                    action = LocationService.ACTION_START_LOCATE
                    putExtra(LocationService.EXTRA_SENDER, sender)
                })
                notify(
                    context, sender, R.string.command_locate,
                    if (replied) R.string.command_reply_sent else R.string.command_reply_not_sent
                )
            }
        }
    }

    private fun evaluateCommand(
        message: String,
        prefs: PreferencesRepository,
        deviceLocked: Boolean,
    ): CommandResult {
        val input = message.trim()
        val normalizedPrefix = prefs.commandPrefix.trim()
        val inputLower = input.lowercase(Locale.ROOT)
        val prefixLower = normalizedPrefix.lowercase(Locale.ROOT)
        if (prefixLower.isBlank() || !inputLower.startsWith(prefixLower)) return CommandResult.Ignored
        val parts = input.split(Regex("\\s+"))
        if (!parts.firstOrNull().equals(normalizedPrefix, ignoreCase = true)) return CommandResult.Ignored
        return when {
            parts.size == 1 || parts[1].equals("ring", ignoreCase = true) -> evaluateRing(parts, prefs, deviceLocked)
            parts[1].equals("locate", ignoreCase = true) -> evaluateLocate(parts, prefs, deviceLocked)
            parts.size == 2 && prefs.ringPassword.isNotBlank() -> evaluateRing(parts, prefs, deviceLocked)
            else -> CommandResult.Ignored
        }
    }

    private fun evaluateRing(
        parts: List<String>,
        prefs: PreferencesRepository,
        deviceLocked: Boolean,
    ): CommandResult {
        val password = prefs.ringPassword.trim()
        val passwordValid = when {
            parts.size == 1 -> password.isBlank()
            parts[1].equals("ring", ignoreCase = true) && password.isBlank() -> parts.size == 2
            parts[1].equals("ring", ignoreCase = true) -> parts.size == 3 && parts[2] == password
            else -> password.isNotBlank() && parts.size == 2 && parts[1] == password
        }
        return evaluate(
            CommandType.RING, prefs.ringEnabled, passwordValid, deviceLocked,
            RejectionReason.INVALID_PASSWORD
        )
    }

    private fun evaluateLocate(
        parts: List<String>,
        prefs: PreferencesRepository,
        deviceLocked: Boolean,
    ): CommandResult {
        val secret = prefs.locateSecret.trim()
        val secretValid = secret.isNotBlank() && parts.size == 3 && parts[2] == secret
        return evaluate(
            CommandType.LOCATE, prefs.locateEnabled, secretValid, deviceLocked,
            RejectionReason.INVALID_SECRET
        )
    }

    private fun evaluate(
        type: CommandType,
        enabled: Boolean,
        credentialValid: Boolean,
        deviceLocked: Boolean,
        invalidCredential: RejectionReason,
    ): CommandResult = when {
        !enabled -> CommandResult.Rejected(type, RejectionReason.DISABLED)
        !credentialValid -> CommandResult.Rejected(type, invalidCredential)
        !deviceLocked -> CommandResult.Rejected(type, RejectionReason.DEVICE_UNLOCKED)
        else -> CommandResult.Accepted(type)
    }

    private sealed interface CommandResult {
        data object Ignored : CommandResult
        data class Rejected(val type: CommandType, val reason: RejectionReason) : CommandResult
        data class Accepted(val type: CommandType) : CommandResult
    }

    private enum class RejectionReason {
        DISABLED, INVALID_PASSWORD, INVALID_SECRET, DEVICE_UNLOCKED
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
