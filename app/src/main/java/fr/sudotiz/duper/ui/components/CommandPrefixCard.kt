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
    commandCode: String,
    prefixError: String?,
    onPrefixChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
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
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = commandCode,
                onValueChange = onCodeChange,
                label = { Text(stringResource(R.string.prefix_code_label)) },
                placeholder = { Text(stringResource(R.string.prefix_code_placeholder)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            val prefix = commandPrefix.trim()
            val code = commandCode.trim()
            val cmdSuffix = if (code.isBlank()) "" else " $code"

            if (prefixError == null) {
                Text(
                    stringResource(R.string.prefix_commands_hint, prefix, cmdSuffix),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
