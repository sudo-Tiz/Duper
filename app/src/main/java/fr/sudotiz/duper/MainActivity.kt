package fr.sudotiz.duper

import android.app.Activity
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.sudotiz.duper.ui.components.BackgroundLocationCard
import fr.sudotiz.duper.ui.components.CommandPrefixCard
import fr.sudotiz.duper.ui.components.GpsCard
import fr.sudotiz.duper.ui.components.LocateModeCard
import fr.sudotiz.duper.ui.components.PermissionsCard
import fr.sudotiz.duper.ui.components.RestrictedSettingsCard
import fr.sudotiz.duper.ui.components.RingModeCard
import fr.sudotiz.duper.ui.components.StatusCard
import fr.sudotiz.duper.util.getRingtoneName

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) {
                DuperApp(viewModel = viewModel())
            }
        }
    }
}

@Composable
fun DuperApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val strDefaultRingtone = stringResource(R.string.ring_default_ringtone)
    val strCustomRingtone = stringResource(R.string.ring_custom_ringtone)
    val strRingPickerTitle = stringResource(R.string.ring_picker_title)

    var ringtoneName by remember(viewModel.ringtoneUri) {
        mutableStateOf(
            viewModel.ringtoneUri?.toUri()
                ?.let { getRingtoneName(context, it, strCustomRingtone) }
                ?: strDefaultRingtone
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshOnResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions -> viewModel.onPermissionsResult(permissions.values.all { it }) }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onBackgroundLocationResult(granted) }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                viewModel.onRingtoneSelected(uri)
                ringtoneName = getRingtoneName(context, uri, strCustomRingtone)
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
            if (!viewModel.hasPermissions) {
                PermissionsCard(
                    onGrantPermissions = {
                        permissionLauncher.launch(MainViewModel.requiredPermissions().toTypedArray())
                    }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    RestrictedSettingsCard()
                }
            }

            if (viewModel.hasPermissions && !viewModel.hasBackgroundLocation &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            ) {
                BackgroundLocationCard(
                    onGrantBackgroundLocation = {
                        backgroundLocationLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                )
            }

            StatusCard(status = viewModel.status)

            CommandPrefixCard(
                commandPrefix = viewModel.commandPrefix,
                commandCode = viewModel.commandCode,
                prefixError = viewModel.prefixError,
                onPrefixChange = viewModel::onPrefixChange,
                onCodeChange = viewModel::onCodeChange,
            )

            RingModeCard(
                ringEnabled = viewModel.ringEnabled,
                ringDuration = viewModel.ringDuration,
                ringDurationError = viewModel.ringDurationError,
                ringtoneName = ringtoneName,
                onRingEnabledChange = viewModel::onRingEnabledChange,
                onDurationChange = viewModel::onRingDurationChange,
                onChooseRingtone = {
                    val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, strRingPickerTitle)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        viewModel.ringtoneUri?.let {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it.toUri())
                        }
                    }
                    ringtoneLauncher.launch(intent)
                }
            )

            LocateModeCard(
                locateEnabled = viewModel.locateEnabled,
                locateDuration = viewModel.locateDuration,
                locateInterval = viewModel.locateInterval,
                locateDurationError = viewModel.locateDurationError,
                locateIntervalError = viewModel.locateIntervalError,
                onLocateEnabledChange = viewModel::onLocateEnabledChange,
                onDurationChange = viewModel::onLocateDurationChange,
                onIntervalChange = viewModel::onLocateIntervalChange,
            )

            GpsCard(
                hasWriteSecureSettings = viewModel.hasWriteSecureSettings,
                isExpanded = viewModel.gpsExpanded,
                onExpandToggle = viewModel::toggleGpsExpanded,
            )
        }
    }
}

