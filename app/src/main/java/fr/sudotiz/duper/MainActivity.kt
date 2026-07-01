package fr.sudotiz.duper

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import fr.sudotiz.duper.data.PreferencesRepository
import fr.sudotiz.duper.ui.components.BackgroundLocationCard
import fr.sudotiz.duper.ui.components.CommandPrefixCard
import fr.sudotiz.duper.ui.components.GpsCard
import fr.sudotiz.duper.ui.components.LocateModeCard
import fr.sudotiz.duper.ui.components.PermissionsCard
import fr.sudotiz.duper.ui.components.RingModeCard
import fr.sudotiz.duper.ui.components.StatusCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) {
                val prefs = (applicationContext as DuperApplication).preferencesRepository
                DuperApp(prefs)
            }
        }
    }
}

@Composable
fun DuperApp(prefs: PreferencesRepository) {
    val context = LocalContext.current

    val strDefaultRingtone = stringResource(R.string.ring_default_ringtone)
    val strPrefixEmpty = stringResource(R.string.error_prefix_empty)
    val strPrefixEmptyCommands = stringResource(R.string.error_prefix_empty_commands)
    val strMinOneSecond = stringResource(R.string.error_min_one_second)
    val strRingPickerTitle = stringResource(R.string.ring_picker_title)
    val strIntervalTooLongTemplate = stringResource(R.string.error_interval_less_than_duration)

    val strCustomRingtone = stringResource(R.string.ring_custom_ringtone)

    fun intervalError(duration: Int) = String.format(strIntervalTooLongTemplate, duration)

    var ringEnabled by remember { mutableStateOf(prefs.ringEnabled) }
    var locateEnabled by remember { mutableStateOf(prefs.locateEnabled) }
    var commandPrefix by remember { mutableStateOf(prefs.commandPrefix) }
    var commandCode by remember { mutableStateOf(prefs.commandCode) }
    var ringDuration by remember { mutableStateOf(prefs.ringDuration.toString()) }
    var locateDuration by remember { mutableStateOf(prefs.locateDuration.toString()) }
    var locateInterval by remember { mutableStateOf(prefs.locateInterval.toString()) }
    var ringtoneUri by remember { mutableStateOf(prefs.ringtoneUri) }
    var ringtoneName by remember {
        mutableStateOf(
            ringtoneUri?.toUri()?.let { getRingtoneName(context, it, strCustomRingtone) } ?: strDefaultRingtone
        )
    }

    var prefixError by remember {
        mutableStateOf(if (prefs.commandPrefix.isBlank()) strPrefixEmpty else null)
    }
    var ringDurationError by remember { mutableStateOf(validatePositiveSeconds(ringDuration, strMinOneSecond)) }
    var locateDurationError by remember { mutableStateOf(validatePositiveSeconds(locateDuration, strMinOneSecond)) }
    var locateIntervalError by remember {
        mutableStateOf(validateLocateInterval(locateInterval, locateDuration, strMinOneSecond, ::intervalError))
    }
    var gpsExpanded by remember { mutableStateOf(false) }

    var lastCommandTime by remember { mutableLongStateOf(prefs.lastCommandTime) }
    var lastCommandSender by remember { mutableStateOf(prefs.lastCommandSender) }
    var lastCommandType by remember { mutableStateOf(prefs.lastCommandType) }
    var lastLocationTime by remember { mutableLongStateOf(prefs.lastLocationTime) }
    var lastLocationLat by remember { mutableStateOf(prefs.lastLocationLat) }
    var lastLocationLng by remember { mutableStateOf(prefs.lastLocationLng) }

    var hasPermissions by remember { mutableStateOf(checkPermissions(context)) }
    var hasBackgroundLocation by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    val hasWriteSecureSettings = remember {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lastCommandTime = prefs.lastCommandTime
                lastCommandSender = prefs.lastCommandSender
                lastCommandType = prefs.lastCommandType
                lastLocationTime = prefs.lastLocationTime
                lastLocationLat = prefs.lastLocationLat
                lastLocationLng = prefs.lastLocationLng
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun persistLocateValuesIfValid(durationValue: String, intervalValue: String) {
        locateDurationError = validatePositiveSeconds(durationValue, strMinOneSecond)
        locateIntervalError = validateLocateInterval(intervalValue, durationValue, strMinOneSecond, ::intervalError)
        if (locateDurationError == null && locateIntervalError == null) {
            prefs.locateDuration = durationValue.toInt()
            prefs.locateInterval = intervalValue.toInt()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasBackgroundLocation = granted }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                ringtoneUri = uri.toString()
                ringtoneName = getRingtoneName(context, uri, strCustomRingtone)
                prefs.ringtoneUri = uri.toString()
            }
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasPermissions) {
                PermissionsCard(
                    onGrantPermissions = { requestPermissions(permissionLauncher) }
                )
            }

            if (hasPermissions && !hasBackgroundLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                BackgroundLocationCard(
                    onGrantBackgroundLocation = {
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                )
            }

            StatusCard(
                lastCommandTime = lastCommandTime,
                lastCommandType = lastCommandType,
                lastCommandSender = lastCommandSender,
                lastLocationTime = lastLocationTime,
                lastLocationLat = lastLocationLat,
                lastLocationLng = lastLocationLng,
                formatTimestamp = ::formatTimestamp
            )

            CommandPrefixCard(
                commandPrefix = commandPrefix,
                commandCode = commandCode,
                prefixError = prefixError,
                onPrefixChange = { newValue ->
                    commandPrefix = newValue
                    if (newValue.isBlank()) {
                        prefixError = strPrefixEmptyCommands
                    } else {
                        prefixError = null
                        prefs.commandPrefix = newValue.trim()
                    }
                },
                onCodeChange = { newValue ->
                    if (!newValue.contains(' ')) {
                        commandCode = newValue
                        prefs.commandCode = newValue.trim()
                    }
                }
            )

            RingModeCard(
                ringEnabled = ringEnabled,
                ringDuration = ringDuration,
                ringDurationError = ringDurationError,
                ringtoneName = ringtoneName,
                onRingEnabledChange = {
                    ringEnabled = it
                    prefs.ringEnabled = it
                },
                onDurationChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { c -> c.isDigit() }) {
                        ringDuration = newValue
                        ringDurationError = validatePositiveSeconds(newValue, strMinOneSecond)
                        val dur = newValue.toIntOrNull()
                        if (ringDurationError == null && dur != null) {
                            prefs.ringDuration = dur
                        }
                    }
                },
                onChooseRingtone = {
                    val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, strRingPickerTitle)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        ringtoneUri?.let {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it.toUri())
                        }
                    }
                    ringtoneLauncher.launch(intent)
                }
            )

            LocateModeCard(
                locateEnabled = locateEnabled,
                locateDuration = locateDuration,
                locateInterval = locateInterval,
                locateDurationError = locateDurationError,
                locateIntervalError = locateIntervalError,
                onLocateEnabledChange = {
                    locateEnabled = it
                    prefs.locateEnabled = it
                },
                onDurationChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { c -> c.isDigit() }) {
                        locateDuration = newValue
                        persistLocateValuesIfValid(newValue, locateInterval)
                    }
                },
                onIntervalChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { c -> c.isDigit() }) {
                        locateInterval = newValue
                        persistLocateValuesIfValid(locateDuration, newValue)
                    }
                }
            )

            GpsCard(
                hasWriteSecureSettings = hasWriteSecureSettings,
                isExpanded = gpsExpanded,
                onExpandToggle = { gpsExpanded = !gpsExpanded }
            )
        }
    }
}

fun formatTimestamp(timeMs: Long): String {
    if (timeMs == 0L) return "Never"
    return SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(timeMs))
}

fun validatePositiveSeconds(value: String, errorMessage: String): String? {
    val seconds = value.toIntOrNull()
    return if (seconds == null || seconds < 1) errorMessage else null
}

fun validateLocateInterval(
    intervalValue: String,
    durationValue: String,
    errorMinOneSecond: String,
    errorIntervalTooLong: (Int) -> String,
): String? {
    val interval = intervalValue.toIntOrNull()
    if (interval == null || interval < 1) return errorMinOneSecond
    val duration = durationValue.toIntOrNull() ?: return null
    return if (interval >= duration) errorIntervalTooLong(duration) else null
}

fun checkPermissions(context: Context): Boolean {
    val required = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        required.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    return required.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

fun requestPermissions(launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
    val permissions = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    launcher.launch(permissions.toTypedArray())
}

fun getRingtoneName(context: Context, uri: Uri, fallback: String = "Custom ringtone"): String {
    return try {
        RingtoneManager.getRingtone(context, uri).getTitle(context)
    } catch (_: Exception) {
        fallback
    }
}
