package fr.sudotiz.duper.util

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log

object SmsUtil {
    private const val TAG = "SmsUtil"

    fun send(context: Context, phoneNumber: String?, message: String) {
        if (phoneNumber.isNullOrBlank()) return
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS", e)
        }
    }
}
