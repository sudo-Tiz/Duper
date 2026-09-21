package fr.sudotiz.duper

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import fr.sudotiz.duper.data.CommandType

data class StatusState(
    val lastCommandTime: Long = 0L,
    val lastCommandSender: String? = null,
    val lastCommandType: CommandType? = null,
    val lastLocationTime: Long = 0L,
    val lastLocationLat: Double? = null,
    val lastLocationLng: Double? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = (application as DuperApplication).preferencesRepository
    private val app = application

    // Settings
    var ringEnabled by mutableStateOf(prefs.ringEnabled); private set
    var locateEnabled by mutableStateOf(prefs.locateEnabled); private set
    var commandPrefix by mutableStateOf(prefs.commandPrefix); private set
    var ringPassword by mutableStateOf(prefs.ringPassword); private set
    var ringReplyEnabled by mutableStateOf(prefs.ringReplyEnabled); private set
    var ringFlashEnabled by mutableStateOf(prefs.ringFlashEnabled); private set
    var ringVibrationEnabled by mutableStateOf(prefs.ringVibrationEnabled); private set
    var ringAudioEnabled by mutableStateOf(prefs.ringAudioEnabled); private set
    var locateSecret by mutableStateOf(prefs.locateSecret); private set
    var ringDuration by mutableStateOf(prefs.ringDuration.toString()); private set
    var locateDuration by mutableStateOf(prefs.locateDuration.toString()); private set
    var locateInterval by mutableStateOf(prefs.locateInterval.toString()); private set
    var ringtoneUri by mutableStateOf(prefs.ringtoneUri); private set

    // Validation errors
    var prefixError by mutableStateOf(
        if (prefs.commandPrefix.isBlank()) app.getString(R.string.error_prefix_empty) else null
    ); private set
    var ringDurationError by mutableStateOf<String?>(null); private set
    var locateDurationError by mutableStateOf<String?>(null); private set
    var locateIntervalError by mutableStateOf<String?>(null); private set
    var locateSecretError by mutableStateOf<String?>(null); private set

    // Status (refreshed on resume)
    var status by mutableStateOf(StatusState(
        lastCommandTime = prefs.lastCommandTime,
        lastCommandSender = prefs.lastCommandSender,
        lastCommandType = prefs.lastCommandType,
        lastLocationTime = prefs.lastLocationTime,
        lastLocationLat = prefs.lastLocationLat,
        lastLocationLng = prefs.lastLocationLng,
    )); private set

    // UI state
    var gpsExpanded by mutableStateOf(false); private set

    // Permissions
    val hasWriteSecureSettings: Boolean = ContextCompat.checkSelfPermission(
        app, Manifest.permission.WRITE_SECURE_SETTINGS
    ) == PackageManager.PERMISSION_GRANTED
    var hasRingPermissions by mutableStateOf(hasPermissions(ringPermissions())); private set
    var hasLocatePermissions by mutableStateOf(hasPermissions(locatePermissions())); private set
    var hasBackgroundLocation by mutableStateOf(checkBackgroundLocationPermission()); private set

    fun refreshOnResume() {
        status = StatusState(
            lastCommandTime = prefs.lastCommandTime,
            lastCommandSender = prefs.lastCommandSender,
            lastCommandType = prefs.lastCommandType,
            lastLocationTime = prefs.lastLocationTime,
            lastLocationLat = prefs.lastLocationLat,
            lastLocationLng = prefs.lastLocationLng,
        )
        hasRingPermissions = hasPermissions(ringPermissions())
        hasLocatePermissions = hasPermissions(locatePermissions())
        hasBackgroundLocation = checkBackgroundLocationPermission()
    }

    fun updateRingEnabled(value: Boolean) { ringEnabled = value; prefs.ringEnabled = value }
    fun updateLocateEnabled(value: Boolean) { locateEnabled = value; prefs.locateEnabled = value }

    fun onPrefixChange(value: String) {
        if (value.any { it.isWhitespace() }) return
        commandPrefix = value
        prefixError = if (value.isBlank()) {
            app.getString(R.string.error_prefix_empty_commands)
        } else {
            prefs.commandPrefix = value.trim()
            null
        }
    }

    fun onRingPasswordChange(value: String) {
        if (!value.contains(' ')) {
            ringPassword = value
            prefs.ringPassword = value.trim()
        }
    }

    fun updateRingReplyEnabled(value: Boolean) {
        ringReplyEnabled = value
        prefs.ringReplyEnabled = value
    }

    fun updateRingFlashEnabled(value: Boolean) {
        ringFlashEnabled = value
        prefs.ringFlashEnabled = value
    }

    fun updateRingVibrationEnabled(value: Boolean) {
        ringVibrationEnabled = value
        prefs.ringVibrationEnabled = value
    }

    fun updateRingAudioEnabled(value: Boolean) {
        ringAudioEnabled = value
        prefs.ringAudioEnabled = value
    }

    fun onLocateSecretChange(value: String) {
        if (!value.contains(' ')) {
            locateSecret = value
            prefs.locateSecret = value.trim()
            locateSecretError = if (value.isBlank()) app.getString(R.string.error_locate_secret_empty) else null
        }
    }

    fun canEnableLocate(): Boolean {
        locateSecretError = if (locateSecret.isBlank()) app.getString(R.string.error_locate_secret_empty) else null
        return locateSecretError == null
    }

    fun onRingDurationChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            ringDuration = value
            ringDurationError = validatePositiveSeconds(value)
            if (ringDurationError == null) prefs.ringDuration = value.toInt()
        }
    }

    fun onLocateDurationChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            locateDuration = value
            persistLocateValuesIfValid(value, locateInterval)
        }
    }

    fun onLocateIntervalChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            locateInterval = value
            persistLocateValuesIfValid(locateDuration, value)
        }
    }

    fun onRingtoneSelected(uri: Uri) {
        ringtoneUri = uri.toString()
        prefs.ringtoneUri = uri.toString()
    }

    fun toggleGpsExpanded() { gpsExpanded = !gpsExpanded }
    fun refreshRingPermissions() { hasRingPermissions = hasPermissions(ringPermissions()) }
    fun refreshLocatePermissions() { hasLocatePermissions = hasPermissions(locatePermissions()) }
    fun onBackgroundLocationResult(granted: Boolean) { hasBackgroundLocation = granted }

    private fun persistLocateValuesIfValid(durationValue: String, intervalValue: String) {
        locateDurationError = validatePositiveSeconds(durationValue)
        locateIntervalError = validateLocateInterval(intervalValue, durationValue)
        if (locateDurationError == null && locateIntervalError == null) {
            prefs.locateDuration = durationValue.toInt()
            prefs.locateInterval = intervalValue.toInt()
        }
    }

    private fun validatePositiveSeconds(value: String): String? {
        val seconds = value.toIntOrNull()
        return if (seconds == null || seconds < 1) app.getString(R.string.error_min_one_second) else null
    }

    private fun validateLocateInterval(intervalValue: String, durationValue: String): String? {
        val interval = intervalValue.toIntOrNull()
        if (interval == null || interval < 1) return app.getString(R.string.error_min_one_second)
        val duration = durationValue.toIntOrNull() ?: return null
        return if (interval >= duration) {
            app.getString(R.string.error_interval_less_than_duration, duration)
        } else null
    }

    private fun hasPermissions(permissions: List<String>): Boolean =
        permissions.all {
            ContextCompat.checkSelfPermission(app, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun checkBackgroundLocationPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                app, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    companion object {
        fun ringPermissions(): List<String> = listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.CAMERA,
        )

        fun locatePermissions(): List<String> = listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}
