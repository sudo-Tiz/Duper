package fr.sudotiz.duper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.sudotiz.duper.R

@Composable
fun CommandPrefixCard(
    commandPrefix: String,
    prefixError: String?,
    onPrefixChange: (String) -> Unit,
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.prefix_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = commandPrefix,
                onValueChange = onPrefixChange,
                label = { Text(stringResource(R.string.prefix_label)) },
                isError = prefixError != null,
                supportingText = {
                    if (prefixError != null) {
                        Text(prefixError, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text(stringResource(R.string.prefix_hint))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            val prefix = commandPrefix.trim()

            if (prefixError == null) {
                Text(
                    stringResource(R.string.prefix_commands_hint, prefix),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
