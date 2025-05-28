package com.example.fitbodstravasyncer.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fitbodstravasyncer.util.LabeledControlConfig
import com.example.fitbodstravasyncer.util.UiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ActionsSheet(
    state: UiState,
    showDeleteAll: () -> Unit,
    onFetch: () -> Unit,
    isFetching: Boolean,
    isChecking: Boolean,
    onCheckMatching: () -> Unit,
    onToggleFutureSync: (Boolean) -> Unit,
    onToggleDailySync: (Boolean) -> Unit,
    onSyncAll: () -> Unit,
    onDateFromChange: (LocalDate?) -> Unit,
    onDateToChange: (LocalDate?) -> Unit,
) {
    val dateFormatter = remember { DateTimeFormatter.ISO_DATE }
    var showDatePickerFrom by remember { mutableStateOf(false) }
    var showDatePickerTo by remember { mutableStateOf(false) }

    // --- DRY control config list ---
    val controls = remember(state, isFetching, isChecking) {
        buildList {
            add(
                LabeledControlConfig(
                    key = "fetch",
                    helpTitle = "Fetch Workouts",
                    helpDescription = "Fetches Fitbod workouts between the selected dates.",
                    content = {
                        Button(
                            onClick = onFetch,
                            enabled = !isFetching && state.dateFrom != null && state.dateTo != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.apiLimitReached) {
                                Text(
                                    text = "Can't Fetch Fitbod. Try again in ${state.apiLimitResetHint}",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            if (isFetching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Text("Fetch Fitbod Workouts")
                        }
                    }
                )
            )
            add(
                LabeledControlConfig(
                    key = "autoSync",
                    helpTitle = "Auto-sync",
                    helpDescription = "Automatically syncs workouts to or from Strava every 15 minutes for the past 24 hours.",
                    label = "Auto-sync 24h every 15m",
                    content = {
                        Switch(
                            checked = state.futureSync,
                            onCheckedChange = onToggleFutureSync
                        )
                    }
                )
            )
            add(
                LabeledControlConfig(
                    key = "dailySync",
                    helpTitle = "Daily Sync",
                    helpDescription = "Performs a daily synchronization of your workouts.",
                    label = "Daily Sync",
                    content = {
                        Switch(
                            checked = state.dailySync,
                            onCheckedChange = onToggleDailySync
                        )
                    }
                )
            )
            add(
                LabeledControlConfig(
                    key = "checkMatching",
                    helpTitle = "Check Matching Workouts",
                    helpDescription = "Checks for workouts that already exist in Strava to avoid duplicates.",
                    content = {
                        Button(
                            onClick = onCheckMatching,
                            enabled = !isChecking,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.apiLimitReached) {
                                Text(
                                    text = "Can't read Strava. Try again in ${state.apiLimitResetHint}",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Text("Read Strava's Activities")
                        }
                    }
                )
            )
            add(
                LabeledControlConfig(
                    key = "syncAll",
                    helpTitle = "Sync All",
                    helpDescription = "Synchronizes all workouts to Strava.",
                    content = {
                        Button(
                            onClick = onSyncAll,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.apiLimitReached) {
                                Text(
                                    text = "Can't sync all. Try again in ${state.apiLimitResetHint}",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Text("Sync All Strava Activities")
                        }
                    }
                )
            )
            if (state.sessionMetrics.isNotEmpty()) {
                add(
                    LabeledControlConfig(
                        key = "deleteAll",
                        helpTitle = "Delete All Sessions from App",
                        helpDescription = "Deletes all workout sessions from the list. Does not delete from Strava neither Fitbod just on this App",
                        content = {
                            Button(
                                onClick = showDeleteAll,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Delete All Activities")
                            }
                        }
                    )
                )
            }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Actions", style = MaterialTheme.typography.titleLarge)

        DatePickerRow(
            label = "Date From",
            value = state.dateFrom?.format(dateFormatter) ?: "Not set",
            onClick = { showDatePickerFrom = true }
        )
        DatePickerRow(
            label = "Date To",
            value = state.dateTo?.format(dateFormatter) ?: "Not set",
            onClick = { showDatePickerTo = true }
        )

        controls.forEach { config ->
            key(config.key) {
                LabeledControlWithHelp(
                    helpTitle = config.helpTitle,
                    helpDescription = config.helpDescription,
                    label = config.label
                ) {
                    config.content()
                }
            }
        }
    }

    if (showDatePickerFrom) {
        MaterialDatePickerDialog(
            initialDate = state.dateFrom ?: LocalDate.now(),
            onDateSelected = { selectedDate ->
                onDateFromChange(selectedDate)
                showDatePickerFrom = false
            },
            onDismiss = { showDatePickerFrom = false }
        )
    }

    if (showDatePickerTo) {
        MaterialDatePickerDialog(
            initialDate = state.dateTo ?: LocalDate.now(),
            onDateSelected = { selectedDate ->
                onDateToChange(selectedDate)
                showDatePickerTo = false
            },
            onDismiss = { showDatePickerTo = false }
        )
    }

}

@Composable
fun LabeledControlWithHelp(
    helpTitle: String,
    helpDescription: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Label (if present)
        label?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        // Main control
        Box(Modifier
            .weight(1f)
            .padding(horizontal = 12.dp), contentAlignment = Alignment.CenterEnd) {
            content()
        }

        // Info icon at the far right
        HelpIconButton(title = helpTitle, description = helpDescription)
    }
}

@Composable
fun HelpIconButton(
    title: String,
    description: String
) {
    var showDialog by remember { mutableStateOf(false) }

    IconButton(onClick = { showDialog = true }) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Help",
            tint = MaterialTheme.colorScheme.primary
        )
    }
    InfoHelpDialog(
        showDialog = showDialog,
        title = title,
        description = description,
        onDismiss = { showDialog = false }
    )
}


@Composable
fun DatePickerRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Button(onClick = onClick) {
            Text("Pick")
        }
    }
}


@Composable
fun SyncActionsSection(
    state: UiState,
    onSyncAll: () -> Unit,
    onCheckMatching: () -> Unit,
    onFetch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Show API limit warning banner if limit is reached
        if (state.apiLimitReached) {
            Text(
                text = "API limit reached. Try again in ${state.apiLimitResetHint}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }

        // Your action buttons
        Button(
            onClick = onSyncAll,
            enabled = !state.apiLimitReached,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sync All")
        }
        Button(
            onClick = onCheckMatching,
            enabled = !state.apiLimitReached,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check Matching")
        }
        Button(
            onClick = onFetch,
            enabled = !state.apiLimitReached,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Fetch Workouts")
        }
    }
}
