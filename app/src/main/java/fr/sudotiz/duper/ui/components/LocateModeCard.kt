package fr.sudotiz.duper.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
fun LocateModeCard(
    locateEnabled: Boolean,
    locateDuration: String,
    locateInterval: String,
    locateDurationError: String?,
    locateIntervalError: String?,
    onLocateEnabledChange: (Boolean) -> Unit,
    onDurationChange: (String) -> Unit,
    onIntervalChange: (String) -> Unit,
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.locate_title), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.locate_enable_label), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = locateEnabled, onCheckedChange = onLocateEnabledChange)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = locateDuration,
                onValueChange = onDurationChange,
                isError = locateDurationError != null,
                label = { Text(stringResource(R.string.locate_duration_label)) },
                supportingText = {
                    if (locateDurationError != null) {
                        Text(locateDurationError, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text(stringResource(R.string.locate_duration_hint))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = locateInterval,
                onValueChange = onIntervalChange,
                isError = locateIntervalError != null,
                label = { Text(stringResource(R.string.locate_interval_label)) },
                supportingText = {
                    if (locateIntervalError != null) {
                        Text(locateIntervalError, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text(stringResource(R.string.locate_interval_hint))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
