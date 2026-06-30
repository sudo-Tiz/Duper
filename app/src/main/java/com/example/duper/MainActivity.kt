package com.example.duper

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) {
                    darkColorScheme()
                } else {
                    lightColorScheme()
                }
            ) {
                DuperApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun DuperApp() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("duper_prefs", Context.MODE_PRIVATE)

    var ringEnabled by remember { mutableStateOf(prefs.getBoolean("ring_enabled", true)) }
    var locateEnabled by remember { mutableStateOf(prefs.getBoolean("locate_enabled", true)) }
    var commandPrefix by remember {
        mutableStateOf(
            prefs.getString("command_prefix", "duper") ?: "duper"
        )
    }
    var commandCode by remember { mutableStateOf(prefs.getString("command_code", "") ?: "") }
    var ringDuration by remember { mutableStateOf(prefs.getInt("ring_duration", 30).toString()) }
    var locateDuration by remember {
        mutableStateOf(
            prefs.getInt("locate_duration", 300).toString()
        )
    }
    var locateInterval by remember {
        mutableStateOf(
            prefs.getInt("locate_interval", 30).toString()
        )
    }
    var ringtoneUri by remember { mutableStateOf(prefs.getString("ringtone_uri", null)) }
    var ringtoneName by remember {
        mutableStateOf(ringtoneUri?.toUri()?.let { getRingtoneName(context, it) }
            ?: "Default alarm")
    }

    var prefixError by remember {
        mutableStateOf(
            if ((prefs.getString("command_prefix", "duper") ?: "duper").isBlank()) {
                "Prefix cannot be empty"
            } else {
                null
            }
        )
    }
    var ringDurationError by remember { mutableStateOf(validatePositiveSeconds(ringDuration)) }
    var locateDurationError by remember { mutableStateOf(validatePositiveSeconds(locateDuration)) }
    var locateIntervalError by remember {
        mutableStateOf(
            validateLocateInterval(
                locateInterval,
                locateDuration
            )
        )
    }
    var gpsExpanded by remember { mutableStateOf(false) }

    var lastCommandTime by remember { mutableLongStateOf(prefs.getLong("last_command_time", 0L)) }
    var lastCommandSender by remember {
        mutableStateOf(
            prefs.getString(
                "last_command_sender",
                null
            )
        )
    }
    var lastCommandType by remember { mutableStateOf(prefs.getString("last_command_type", null)) }
    var lastLocationTime by remember { mutableLongStateOf(prefs.getLong("last_location_time", 0L)) }
    var lastLocationLat by remember { mutableStateOf(prefs.getString("last_location_lat", null)) }
    var lastLocationLng by remember { mutableStateOf(prefs.getString("last_location_lng", null)) }

    var hasPermissions by remember { mutableStateOf(checkPermissions(context)) }
    var hasBackgroundLocation by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    val hasWriteSecureSettings = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lastCommandTime = prefs.getLong("last_command_time", 0L)
                lastCommandSender = prefs.getString("last_command_sender", null)
                lastCommandType = prefs.getString("last_command_type", null)
                lastLocationTime = prefs.getLong("last_location_time", 0L)
                lastLocationLat = prefs.getString("last_location_lat", null)
                lastLocationLng = prefs.getString("last_location_lng", null)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun persistLocateValuesIfValid(durationValue: String, intervalValue: String) {
        locateDurationError = validatePositiveSeconds(durationValue)
        locateIntervalError = validateLocateInterval(intervalValue, durationValue)
        if (locateDurationError == null && locateIntervalError == null) {
            prefs.edit {
                putInt("locate_duration", durationValue.toInt())
                putInt("locate_interval", intervalValue.toInt())
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBackgroundLocation = granted
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    Uri::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                ringtoneUri = uri.toString()
                ringtoneName = getRingtoneName(context, uri)
                prefs.edit { putString("ringtone_uri", uri.toString()) }
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Permissions Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "The app needs permissions to function.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                requestPermissions(permissionLauncher)
                            }
                        ) {
                            Text("Grant Permissions")
                        }
                    }
                }
            }

            if (hasPermissions && !hasBackgroundLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Background Location (Optional)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "For better location tracking when screen is off.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            }
                        ) {
                            Text("Grant Background Location")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "📡 Status",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (lastCommandTime == 0L) {
                            "Last command received: Never"
                        } else {
                            "Last command received: ${lastCommandType ?: "unknown"} from ${lastCommandSender ?: "unknown"}\n${
                                formatTimestamp(
                                    lastCommandTime
                                )
                            }"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (lastLocationTime == 0L || lastLocationLat == null || lastLocationLng == null) {
                            "Last position sent: Never"
                        } else {
                            "Last position sent: $lastLocationLat, $lastLocationLng\n${
                                formatTimestamp(
                                    lastLocationTime
                                )
                            }"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Command Prefix", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = commandPrefix,
                        onValueChange = { newValue ->
                            commandPrefix = newValue
                            if (newValue.isBlank()) {
                                prefixError = "Prefix cannot be empty — commands would break"
                            } else {
                                prefixError = null
                                prefs.edit { putString("command_prefix", newValue.trim()) }
                            }
                        },
                        label = { Text("Prefix (e.g., 'duper')") },
                        isError = prefixError != null,
                        supportingText = {
                            if (prefixError != null) {
                                Text(prefixError!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = commandCode,
                        onValueChange = { newValue ->
                            if (!newValue.contains(' ')) {
                                commandCode = newValue
                                prefs.edit { putString("command_code", newValue.trim()) }
                            }
                        },
                        label = { Text("Password (optional)") },
                        placeholder = { Text("e.g., abc123") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val prefix = commandPrefix.trim()
                    val code = commandCode.trim()
                    val cmdSuffix = if (code.isBlank()) "" else " $code"

                    if (prefixError == null) {
                        Text(
                            "Commands: \"$prefix ring$cmdSuffix\" · \"$prefix locate$cmdSuffix\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ring Mode", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Ring Mode", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = ringEnabled,
                            onCheckedChange = {
                                ringEnabled = it
                                prefs.edit { putBoolean("ring_enabled", it) }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = ringDuration,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.all { c -> c.isDigit() }) {
                                ringDuration = newValue
                                ringDurationError = validatePositiveSeconds(newValue)
                                val dur = newValue.toIntOrNull()
                                if (ringDurationError == null && dur != null) {
                                    prefs.edit { putInt("ring_duration", dur) }
                                }
                            }
                        },
                        isError = ringDurationError != null,
                        label = { Text("Ring Duration (seconds)") },
                        supportingText = {
                            if (ringDurationError != null) {
                                Text(ringDurationError!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Current ringtone: $ringtoneName",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_TYPE,
                                    RingtoneManager.TYPE_ALARM
                                )
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose Ringtone")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                ringtoneUri?.let {
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                        it.toUri()
                                    )
                                }
                            }
                            ringtoneLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Choose Ringtone")
                    }
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Locate Mode", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Locate Mode", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = locateEnabled,
                            onCheckedChange = {
                                locateEnabled = it
                                prefs.edit { putBoolean("locate_enabled", it) }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = locateDuration,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.all { c -> c.isDigit() }) {
                                locateDuration = newValue
                                persistLocateValuesIfValid(newValue, locateInterval)
                            }
                        },
                        isError = locateDurationError != null,
                        label = { Text("Total Duration (seconds)") },
                        supportingText = {
                            if (locateDurationError != null) {
                                Text(locateDurationError!!, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("How long to track location")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = locateInterval,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.all { c -> c.isDigit() }) {
                                locateInterval = newValue
                                persistLocateValuesIfValid(locateDuration, newValue)
                            }
                        },
                        isError = locateIntervalError != null,
                        label = { Text("Send Interval (seconds)") },
                        supportingText = {
                            if (locateIntervalError != null) {
                                Text(locateIntervalError!!, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("How often to send location")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (hasWriteSecureSettings) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { gpsExpanded = !gpsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (hasWriteSecureSettings) "✅ Auto-enable GPS" else "Auto-enable GPS (Advanced)",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (hasWriteSecureSettings) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            }
                        )
                        Text(
                            if (gpsExpanded) "▲" else "▼",
                            color = if (hasWriteSecureSettings) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            }
                        )
                    }
                    if (gpsExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (hasWriteSecureSettings) {
                            Text(
                                "The app can turn on location automatically when a 'locate' command is received.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Text(
                                "By default, Android does not let apps turn on location. To allow Duper to enable it automatically, grant a special permission once via ADB:\n\n" +
                                        "1. On a computer, install Android Platform Tools (ADB).\n" +
                                        "2. Enable USB debugging on this phone (Settings > Developer options).\n" +
                                        "3. Connect the phone via USB.\n" +
                                        "4. Run this command:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SelectionContainer {
                                Text(
                                    "adb shell pm grant fr.sudotiz.duper android.permission.WRITE_SECURE_SETTINGS",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "This only needs to be done once. The permission persists after reboots. Without it, Duper will reply by SMS that location is off instead of enabling it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

        }
    }
}

fun formatTimestamp(timeMs: Long): String {
    if (timeMs == 0L) return "Never"
    return SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(timeMs))
}

fun validatePositiveSeconds(value: String): String? {
    val seconds = value.toIntOrNull()
    return if (seconds == null || seconds < 1) "Must be at least 1 second" else null
}

fun validateLocateInterval(intervalValue: String, durationValue: String): String? {
    val interval = intervalValue.toIntOrNull()
    if (interval == null || interval < 1) {
        return "Must be at least 1 second"
    }

    val duration = durationValue.toIntOrNull() ?: return null
    return if (interval >= duration) {
        "Interval must be less than duration (${duration}s)"
    } else {
        null
    }
}

fun checkPermissions(context: Context): Boolean {
    val requiredPermissions = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    return requiredPermissions.all {
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

fun getRingtoneName(context: Context, uri: Uri): String {
    return try {
        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone.getTitle(context)
    } catch (_: Exception) {
        "Custom ringtone"
    }
}
