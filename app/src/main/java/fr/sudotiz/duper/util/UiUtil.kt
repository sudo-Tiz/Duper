package fr.sudotiz.duper.util

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatTimestamp(timeMs: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(timeMs))

fun getRingtoneName(context: Context, uri: Uri, fallback: String): String =
    try {
        RingtoneManager.getRingtone(context, uri).getTitle(context)
    } catch (_: Exception) {
        fallback
    }
