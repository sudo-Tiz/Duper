package fr.sudotiz.duper.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.sudotiz.duper.R

@Composable
fun RingModeCard(
    ringEnabled: Boolean,
    ringPassword: String,
    ringReplyEnabled: Boolean,
    ringFlashEnabled: Boolean,
    ringVibrationEnabled: Boolean,
    ringAudioEnabled: Boolean,
    ringDuration: String,
    ringDurationError: String?,
    ringtoneName: String,
    onRingEnabledChange: (Boolean) -> Unit,
    onPasswordChange: (String) -> Unit,
    onReplyEnabledChange: (Boolean) -> Unit,
    onFlashEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onAudioEnabledChange: (Boolean) -> Unit,
    onDurationChange: (String) -> Unit,
    onChooseRingtone: () -> Unit,
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.ring_title), style = MaterialTheme.typography.titleLarge)
                Switch(checked = ringEnabled, onCheckedChange = onRingEnabledChange)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = ringPassword,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.ring_password_label)) },
                placeholder = { Text(stringResource(R.string.ring_password_placeholder)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                stringResource(R.string.ring_commands_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.ring_reply_label), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = ringReplyEnabled, onCheckedChange = onReplyEnabledChange)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.ring_flash_label), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = ringFlashEnabled, onCheckedChange = onFlashEnabledChange)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.ring_vibration_label), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = ringVibrationEnabled, onCheckedChange = onVibrationEnabledChange)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.ring_audio_label), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = ringAudioEnabled, onCheckedChange = onAudioEnabledChange)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = ringDuration,
                onValueChange = onDurationChange,
                isError = ringDurationError != null,
                label = { Text(stringResource(R.string.ring_duration_label)) },
                supportingText = {
                    if (ringDurationError != null) {
                        Text(ringDurationError, color = MaterialTheme.colorScheme.error)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                stringResource(R.string.ring_current_ringtone, ringtoneName),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onChooseRingtone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ring_choose_ringtone))
            }
        }
    }
}
