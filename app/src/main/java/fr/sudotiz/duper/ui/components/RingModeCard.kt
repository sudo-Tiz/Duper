package fr.sudotiz.duper.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import fr.sudotiz.duper.R

@Composable
fun RingModeCard(
    ringEnabled: Boolean,
    ringDuration: String,
    ringDurationError: String?,
    ringtoneName: String,
    onRingEnabledChange: (Boolean) -> Unit,
    onDurationChange: (String) -> Unit,
    onChooseRingtone: () -> Unit,
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.ring_title), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.ring_enable_label), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = ringEnabled, onCheckedChange = onRingEnabledChange)
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
