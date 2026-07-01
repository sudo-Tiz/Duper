package fr.sudotiz.duper

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = (application as DuperApplication).preferencesRepository
    private val app = application

    // Settings
    var ringEnabled by mutableStateOf(prefs.ringEnabled); private set
    var locateEnabled by mutableStateOf(prefs.locateEnabled); private set
    var commandPrefix by mutableStateOf(prefs.commandPrefix); private set
    var commandCode by mutableStateOf(prefs.commandCode); private set
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

    // Status (refreshed on resume)
    var lastCommandTime by mutableLongStateOf(prefs.lastCommandTime); private set
    var lastCommandSender by mutableStateOf(prefs.lastCommandSender); private set
    var lastCommandType by mutableStateOf(prefs.lastCommandType); private set
    var lastLocationTime by mutableLongStateOf(prefs.lastLocationTime); private set
    var lastLocationLat by mutableStateOf(prefs.lastLocationLat); private set
    var lastLocationLng by mutableStateOf(prefs.lastLocationLng); private set

    // UI state
    var gpsExpanded by mutableStateOf(false); private set

    // Permissions
    val hasWriteSecureSettings: Boolean = ContextCompat.checkSelfPermission(
        app, Manifest.permission.WRITE_SECURE_SETTINGS
    ) == PackageManager.PERMISSION_GRANTED
    var hasPermissions by mutableStateOf(checkRequiredPermissions()); private set
    var hasBackgroundLocation by mutableStateOf(checkBackgroundLocationPermission()); private set

    fun refreshOnResume() {
        lastCommandTime = prefs.lastCommandTime
        lastCommandSender = prefs.lastCommandSender
        lastCommandType = prefs.lastCommandType
        lastLocationTime = prefs.lastLocationTime
        lastLocationLat = prefs.lastLocationLat
        lastLocationLng = prefs.lastLocationLng
        hasPermissions = checkRequiredPermissions()
        hasBackgroundLocation = checkBackgroundLocationPermission()
    }

    fun onRingEnabledChange(value: Boolean) { ringEnabled = value; prefs.ringEnabled = value }
    fun onLocateEnabledChange(value: Boolean) { locateEnabled = value; prefs.locateEnabled = value }

    fun onPrefixChange(value: String) {
        commandPrefix = value
        prefixError = if (value.isBlank()) {
            app.getString(R.string.error_prefix_empty_commands)
        } else {
            prefs.commandPrefix = value.trim()
            null
        }
    }

    fun onCodeChange(value: String) {
        if (!value.contains(' ')) {
            commandCode = value
            prefs.commandCode = value.trim()
        }
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
    fun onPermissionsResult(allGranted: Boolean) { hasPermissions = allGranted }
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

    private fun checkRequiredPermissions(): Boolean =
        requiredPermissions().all {
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
        fun requiredPermissions(): List<String> = buildList {
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
