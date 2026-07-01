package fr.sudotiz.duper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.sudotiz.duper.R
import fr.sudotiz.duper.StatusState
import fr.sudotiz.duper.util.formatTimestamp

@Composable
fun StatusCard(status: StatusState) {
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
                if (status.lastCommandTime == 0L) {
                    stringResource(R.string.status_last_command_never)
                } else {
                    stringResource(
                        R.string.status_last_command,
                        status.lastCommandType?.label ?: stringResource(R.string.unknown),
                        status.lastCommandSender ?: stringResource(R.string.unknown),
                        formatTimestamp(status.lastCommandTime)
                    )
                },
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (status.lastLocationTime == 0L || status.lastLocationLat == null || status.lastLocationLng == null) {
                    stringResource(R.string.status_last_position_never)
                } else {
                    stringResource(
                        R.string.status_last_position,
                        status.lastLocationLat,
                        status.lastLocationLng,
                        formatTimestamp(status.lastLocationTime)
                    )
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
