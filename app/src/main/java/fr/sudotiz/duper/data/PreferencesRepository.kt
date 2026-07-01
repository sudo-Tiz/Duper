package fr.sudotiz.duper.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PreferenceKeys.PREFS_NAME, Context.MODE_PRIVATE)

    var ringEnabled: Boolean
        get() = prefs.getBoolean(PreferenceKeys.RING_ENABLED, true)
        set(value) = prefs.edit { putBoolean(PreferenceKeys.RING_ENABLED, value) }

    var locateEnabled: Boolean
        get() = prefs.getBoolean(PreferenceKeys.LOCATE_ENABLED, true)
        set(value) = prefs.edit { putBoolean(PreferenceKeys.LOCATE_ENABLED, value) }

    var commandPrefix: String
        get() = prefs.getString(PreferenceKeys.COMMAND_PREFIX, DEFAULT_PREFIX) ?: DEFAULT_PREFIX
        set(value) = prefs.edit { putString(PreferenceKeys.COMMAND_PREFIX, value) }

    var commandCode: String
        get() = prefs.getString(PreferenceKeys.COMMAND_CODE, "") ?: ""
        set(value) = prefs.edit { putString(PreferenceKeys.COMMAND_CODE, value) }

    var ringDuration: Int
        get() = prefs.getInt(PreferenceKeys.RING_DURATION, DEFAULT_RING_DURATION)
        set(value) = prefs.edit { putInt(PreferenceKeys.RING_DURATION, value) }

    var locateDuration: Int
        get() = prefs.getInt(PreferenceKeys.LOCATE_DURATION, DEFAULT_LOCATE_DURATION)
        set(value) = prefs.edit { putInt(PreferenceKeys.LOCATE_DURATION, value) }

    var locateInterval: Int
        get() = prefs.getInt(PreferenceKeys.LOCATE_INTERVAL, DEFAULT_LOCATE_INTERVAL)
        set(value) = prefs.edit { putInt(PreferenceKeys.LOCATE_INTERVAL, value) }

    var ringtoneUri: String?
        get() = prefs.getString(PreferenceKeys.RINGTONE_URI, null)
        set(value) = prefs.edit { putString(PreferenceKeys.RINGTONE_URI, value) }

    var lastCommandTime: Long
        get() = prefs.getLong(PreferenceKeys.LAST_COMMAND_TIME, 0L)
        set(value) = prefs.edit { putLong(PreferenceKeys.LAST_COMMAND_TIME, value) }

    var lastCommandSender: String?
        get() = prefs.getString(PreferenceKeys.LAST_COMMAND_SENDER, null)
        set(value) = prefs.edit { putString(PreferenceKeys.LAST_COMMAND_SENDER, value) }

    var lastCommandType: String?
        get() = prefs.getString(PreferenceKeys.LAST_COMMAND_TYPE, null)
        set(value) = prefs.edit { putString(PreferenceKeys.LAST_COMMAND_TYPE, value) }

    var lastLocationTime: Long
        get() = prefs.getLong(PreferenceKeys.LAST_LOCATION_TIME, 0L)
        set(value) = prefs.edit { putLong(PreferenceKeys.LAST_LOCATION_TIME, value) }

    var lastLocationLat: String?
        get() = prefs.getString(PreferenceKeys.LAST_LOCATION_LAT, null)
        set(value) = prefs.edit { putString(PreferenceKeys.LAST_LOCATION_LAT, value) }

    var lastLocationLng: String?
        get() = prefs.getString(PreferenceKeys.LAST_LOCATION_LNG, null)
        set(value) = prefs.edit { putString(PreferenceKeys.LAST_LOCATION_LNG, value) }

    fun recordCommand(type: String, sender: String) {
        prefs.edit {
            putLong(PreferenceKeys.LAST_COMMAND_TIME, System.currentTimeMillis())
            putString(PreferenceKeys.LAST_COMMAND_SENDER, sender)
            putString(PreferenceKeys.LAST_COMMAND_TYPE, type)
        }
    }

    fun recordLocation(lat: Double, lng: Double) {
        prefs.edit {
            putLong(PreferenceKeys.LAST_LOCATION_TIME, System.currentTimeMillis())
            putString(PreferenceKeys.LAST_LOCATION_LAT, lat.toString())
            putString(PreferenceKeys.LAST_LOCATION_LNG, lng.toString())
        }
    }

    companion object {
        const val DEFAULT_PREFIX = "duper"
        const val DEFAULT_RING_DURATION = 30
        const val DEFAULT_LOCATE_DURATION = 300
        const val DEFAULT_LOCATE_INTERVAL = 30
    }
}
