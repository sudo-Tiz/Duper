package fr.sudotiz.duper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import fr.sudotiz.duper.R

@Composable
fun StatusCard(
    lastCommandTime: Long,
    lastCommandType: String?,
    lastCommandSender: String?,
    lastLocationTime: Long,
    lastLocationLat: String?,
    lastLocationLng: String?,
    formatTimestamp: (Long) -> String,
) {
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
            Text(stringResource(R.string.status_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (lastCommandTime == 0L) {
                    stringResource(R.string.status_last_command_never)
                } else {
                    stringResource(
                        R.string.status_last_command,
                        lastCommandType ?: stringResource(R.string.unknown),
                        lastCommandSender ?: stringResource(R.string.unknown),
                        formatTimestamp(lastCommandTime)
                    )
                },
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (lastLocationTime == 0L || lastLocationLat == null || lastLocationLng == null) {
                    stringResource(R.string.status_last_position_never)
                } else {
                    stringResource(
                        R.string.status_last_position,
                        lastLocationLat,
                        lastLocationLng,
                        formatTimestamp(lastLocationTime)
                    )
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
