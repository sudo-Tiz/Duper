package com.example.duper

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log

object SmsUtil {
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
            Log.e("SmsUtil", "Error sending SMS", e)
        }
    }
}
