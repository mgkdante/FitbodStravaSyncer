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
import com.example.fitbodstravasyncer.ui.UiStrings
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
                    helpTitle = UiStrings.FETCH_WORKOUTS_TITLE,
                    helpDescription = UiStrings.FETCH_WORKOUTS_DESC,
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
                            Text(UiStrings.FETCH_FITBOD_BTN)
                        }
                    }
                )
            )
            add(
                LabeledControlConfig(
                    key = "autoSync",
                    helpTitle = UiStrings.AUTO_SYNC_TITLE,
                    helpDescription = UiStrings.AUTO_SYNC_DESC,
                    label = UiStrings.AUTO_SYNC_LABEL,
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
                    helpTitle = UiStrings.DAILY_SYNC_LABEL,
                    helpDescription = UiStrings.DAILY_SYNC_DESC,
                    label = UiStrings.DAILY_SYNC_LABEL,
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
                    helpTitle = UiStrings.CHECK_MATCHING_TITLE,
                    helpDescription = UiStrings.CHECK_MATCHING_DESC,
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
                            Text(UiStrings.CHECK_MATCHING_BTN)
                        }
                    }
                )
            )
            add(
                LabeledControlConfig(
                    key = "syncAll",
                    helpTitle = UiStrings.SYNC_ALL_TITLE,
                    helpDescription = UiStrings.SYNC_ALL_DESC,
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
                            Text(UiStrings.SYNC_ALL_BTN)
                        }
                    }
                )
            )
            if (state.sessionMetrics.isNotEmpty()) {
                add(
                    LabeledControlConfig(
                        key = "deleteAll",
                        helpTitle = UiStrings.DELETE_ALL_TITLE,
                        helpDescription = UiStrings.DELETE_ALL_DESC,
                        content = {
                            Button(
                                onClick = showDeleteAll,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(UiStrings.DELETE_ALL_BTN)
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
        Text(UiStrings.ACTIONS, style = MaterialTheme.typography.titleLarge)

        DatePickerRow(
            label = UiStrings.DATE_FROM,
            value = state.dateFrom?.format(dateFormatter) ?: UiStrings.NOT_SET,
            onClick = { showDatePickerFrom = true }
        )
        DatePickerRow(
            label = UiStrings.DATE_TO,
            value = state.dateTo?.format(dateFormatter) ?: UiStrings.NOT_SET,
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
            contentDescription = UiStrings.HELP,
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
            Text(UiStrings.PICK)
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
            Text(UiStrings.SYNC_ALL_TITLE)
        }
        Button(
            onClick = onCheckMatching,
            enabled = !state.apiLimitReached,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(UiStrings.CHECK_MATCHING_BUTTON)
        }
        Button(
            onClick = onFetch,
            enabled = !state.apiLimitReached,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(UiStrings.FETCH_WORKOUTS_TITLE)
        }
    }
}
