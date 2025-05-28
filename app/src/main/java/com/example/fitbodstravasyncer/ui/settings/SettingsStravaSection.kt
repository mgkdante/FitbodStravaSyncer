package com.example.fitbodstravasyncer.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fitbodstravasyncer.util.UiState
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun SettingsStravaSection(
    isStravaConnected: Boolean,
    uiState: UiState,
    onDisconnectStrava: () -> Unit,
    onConnectStrava: (() -> Unit)? = null // <-- NEW: optional callback
) {
    Card(
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            StravaSectionHeader()
            ApiUsageBanner(uiState = uiState)
            StravaConnectionState(
                isConnected = isStravaConnected,
                onDisconnectStrava = onDisconnectStrava,
                onConnectStrava = onConnectStrava
            )
        }
    }
}

@Composable
private fun StravaSectionHeader() {
    Text(
        text = "Strava",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun StravaConnectionState(
    isConnected: Boolean,
    onDisconnectStrava: () -> Unit,
    onConnectStrava: (() -> Unit)? = null
) {
    if (isConnected) {
        DisconnectStravaButton(onDisconnectStrava = onDisconnectStrava)
    } else {
        StravaNotConnectedRow(onConnectStrava)
    }
}

@Composable
private fun DisconnectStravaButton(onDisconnectStrava: () -> Unit) {
    Button(
        onClick = onDisconnectStrava,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.LinkOff, contentDescription = "Disconnect")
        Spacer(Modifier.width(8.dp))
        Text("Disconnect from Strava")
    }
}

@Composable
private fun StravaNotConnectedRow(onConnectStrava: (() -> Unit)?) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Not connected to Strava",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        if (onConnectStrava != null) {
            Button(
                onClick = onConnectStrava,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reconnect Strava")
            }
        }
    }
}

@Composable
fun ApiUsageBanner(uiState: UiState) {
    var showInfoDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Warning banner if near the limit
        if (uiState.userApiWarning) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "You're close to your Strava API usage limit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        // Always show usage info in a subtle surface
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.userApiUsageString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showInfoDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "API Usage Info",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showInfoDialog) {
        ApiUsageInfoDialog(onDismiss = { showInfoDialog = false })
    }
}

@Composable
fun ApiUsageInfoDialog(onDismiss: () -> Unit) {
    // Calculate the next reset time at midnight UTC
    val nextResetUtc = ZonedDateTime.now(ZoneOffset.UTC)
        .plusDays(1)
        .toLocalDate()
        .atStartOfDay(ZoneOffset.UTC)

    // Convert to the user's local time zone
    val nextResetLocal = nextResetUtc.withZoneSameInstant(ZoneId.systemDefault())

    // Format the time for display
    val formattedTime = nextResetLocal.format(
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Strava API Usage") },
        text = {
            Text(
                "Strava's API usage limits reset daily at midnight UTC, which is $formattedTime your local time. " +
                        "Your local usage counters are synchronized accordingly to reflect this schedule."
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}
