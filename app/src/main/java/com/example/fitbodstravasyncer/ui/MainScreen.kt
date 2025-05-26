// MainScreen.kt

package com.example.fitbodstravasyncer.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitbodstravasyncer.AppThemeMode
import com.example.fitbodstravasyncer.viewmodel.MainViewModel
import com.example.fitbodstravasyncer.viewmodel.SessionMetrics
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    appThemeMode: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }

    val selectedIds = remember { mutableStateListOf<String>() }
    var showDelete by remember { mutableStateOf(false) }
    var showDeleteAll by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fitbod → Strava") },
                actions = {
                    ThemeToggleRow(selectedMode = appThemeMode, onModeSelected = onThemeChange)
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.sessionMetrics) { session ->
                    SessionCardWithCheckbox(
                        session = session,
                        checked = selectedIds.contains(session.id),
                        onCheckedChange = { checked ->
                            if (checked) selectedIds.add(session.id)
                            else selectedIds.remove(session.id)
                        }
                    )
                }
            }

            if (state.isFetching) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                }
            }

            if (showDelete) {
                AlertDialog(
                    onDismissRequest = { showDelete = false },
                    title = { Text("Confirm Delete") },
                    text = { Text("Delete ${selectedIds.size} sessions?") },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.deleteSessions(selectedIds.toList())
                            selectedIds.clear()
                            showDelete = false
                        }) { Text("Yes") }
                    },
                    dismissButton = {
                        Button(onClick = { showDelete = false }) { Text("No") }
                    }
                )
            }
            if (showDeleteAll) {
                AlertDialog(
                    onDismissRequest = { showDeleteAll = false },
                    title = { Text("Confirm Delete All") },
                    text = { Text("Delete ALL sessions?") },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.deleteAllSessions()
                            selectedIds.clear()
                            showDeleteAll = false
                        }) { Text("Yes") }
                    },
                    dismissButton = {
                        Button(onClick = { showDeleteAll = false }) { Text("No") }
                    }
                )
            }

            if (showSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSheet = false },
                    sheetState = sheetState,
                    contentWindowInsets = { WindowInsets(0) }
                ) {
                    ActionsSheet(
                        state = state,
                        selectedIds = selectedIds,
                        showDelete = { showDelete = true },
                        showDeleteAll = { showDeleteAll = true },
                        onFetch = {
                            coroutineScope.launch {
                                viewModel.fetchSessions(state.dateFrom, state.dateTo)
                            }
                            showSheet = false
                        },
                        isFetching = state.isFetching,
                        onCheckMatching = {
                            isChecking = true
                            viewModel.restoreStravaIds()
                            Toast.makeText(context, "Matching Strava workouts checked.", Toast.LENGTH_SHORT).show()
                            isChecking = false
                            showSheet = false
                        },
                        onToggleFutureSync = viewModel::toggleFutureSync,
                        onToggleDailySync = viewModel::toggleDailySync,
                        onSyncAll = {
                            viewModel.enqueueSyncAll()
                            showSheet = false
                        },
                        onDateFromChange = viewModel::setDateFrom,
                        onDateToChange = viewModel::setDateTo,
                    )
                }
            }

            // Independent Floating Action Buttons
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Trash FAB at BottomStart
                if (selectedIds.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { showDelete = true },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                    }
                }

                // More Actions FAB at BottomEnd
                FloatingActionButton(
                    onClick = { showSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                }
            }
        }
    }
}


@Composable
fun SessionCardWithCheckbox(
    session: SessionMetrics,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer
        )
    )

    // Preprocess the dateTime string
    val rawDateTime = session.dateTime
        .replace(".", "") // Remove periods from AM/PM
        .replace("–", "-") // Replace en dash with hyphen

    // Split the dateTime string into start and end parts
    val dateTimeParts = rawDateTime.split(" - ")
    val startPart = dateTimeParts.getOrNull(0) ?: ""
    val endPart = dateTimeParts.getOrNull(1) ?: ""

    // Extract date and time from the start part
    val startDateTimeParts = startPart.split(" ")
    val date = startDateTimeParts.getOrNull(0) ?: ""
    val startTimeStr = startDateTimeParts.drop(1).joinToString(" ")

    // The end part contains only the time
    val endDateTimeParts = endPart.split(" ")
    val endTimeStr = endDateTimeParts.drop(1).joinToString(" ")

    // Define the time formatter
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    // Parse the start and end times
    val startTime = try {
        LocalTime.parse(startTimeStr, timeFormatter)
    } catch (e: Exception) {
        null
    }

    val endTime = try {
        LocalTime.parse(endTimeStr, timeFormatter)
    } catch (e: Exception) {
        null
    }

    // Format the times for display
    val formattedStartTime = startTime?.format(timeFormatter) ?: startTimeStr
    val formattedEndTime = endTime?.format(timeFormatter) ?: endTimeStr

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .background(backgroundGradient)
                .padding(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Title
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                // Date
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                // Time Range
                Text(
                    text = "$formattedStartTime - $formattedEndTime",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                // Description
                session.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Metrics Row
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Duration
                    StatColumn(
                        icon = Icons.Default.AccessTime,
                        label = "Duration",
                        value = "${session.activeTime} min",
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                    // Calories
                    StatColumn(
                        icon = Icons.Default.LocalFireDepartment,
                        label = "Calories",
                        value = "${session.calories.toInt()} kcal",
                        iconTint = MaterialTheme.colorScheme.error
                    )
                    // Avg Heart Rate
                    session.avgHeartRate?.let {
                        StatColumn(
                            icon = Icons.Default.Favorite,
                            label = "Avg HR",
                            value = "${it.toInt()} bpm",
                            iconTint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Checkbox positioned at the top end
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
            )
        }
    }
}


@Composable
fun ActionsSheet(
    state: com.example.fitbodstravasyncer.viewmodel.UiState,
    selectedIds: List<String>,
    showDelete: () -> Unit,
    showDeleteAll: () -> Unit,
    onFetch: () -> Unit,
    isFetching: Boolean,
    onCheckMatching: () -> Unit,
    onToggleFutureSync: (Boolean) -> Unit,
    onToggleDailySync: (Boolean) -> Unit,
    onSyncAll: () -> Unit,
    onDateFromChange: (LocalDate?) -> Unit,
    onDateToChange: (LocalDate?) -> Unit,
) {
    val dateFormatter = DateTimeFormatter.ISO_DATE
    var showDatePickerFrom by remember { mutableStateOf(false) }
    var showDatePickerTo by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Actions", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Date From", style = MaterialTheme.typography.labelSmall)
                Text(
                    state.dateFrom?.format(dateFormatter) ?: "Not set",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            OutlinedButton(onClick = { showDatePickerFrom = true }) {
                Text("Pick")
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Date To", style = MaterialTheme.typography.labelSmall)
                Text(
                    state.dateTo?.format(dateFormatter) ?: "Not set",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            OutlinedButton(onClick = { showDatePickerTo = true }) {
                Text("Pick")
            }
        }
        Button(
            onClick = onFetch,
            enabled = !isFetching && state.dateFrom != null && state.dateTo != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isFetching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("Fetch Workouts")
        }

        if (state.sessionMetrics.isNotEmpty()) {
            OutlinedButton(
                onClick = showDeleteAll,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete All")
            }
        }
        Button(
            onClick = onCheckMatching,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check matching Strava workouts")
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Auto-sync 24h every 15m", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = state.futureSync,
                onCheckedChange = onToggleFutureSync
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Daily Sync", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = state.dailySync,
                onCheckedChange = onToggleDailySync
            )
        }

        Button(
            onClick = onSyncAll,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sync All")
        }
    }

    if (showDatePickerFrom) {
        MaterialDatePickerDialog(
            initialDate = state.dateFrom ?: LocalDate.now(),
            onDateSelected = {
                onDateFromChange(it)
                showDatePickerFrom = false
            },
            onDismiss = { showDatePickerFrom = false }
        )
    }

    if (showDatePickerTo) {
        MaterialDatePickerDialog(
            initialDate = state.dateTo ?: LocalDate.now(),
            onDateSelected = {
                onDateToChange(it)
                showDatePickerTo = false
            },
            onDismiss = { showDatePickerTo = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val selectedDate = Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    onDateSelected(selectedDate)
                }
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}


@Composable
fun ThemeToggleRow(
    selectedMode: AppThemeMode,
    onModeSelected: (AppThemeMode) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(end = 8.dp)
    ) {
        SegmentedButton(
            icon = Icons.Default.Brightness7,
            label = "Light",
            selected = selectedMode == AppThemeMode.LIGHT,
            onClick = { onModeSelected(AppThemeMode.LIGHT) }
        )
        SegmentedButton(
            icon = Icons.Default.Brightness4,
            label = "Dark",
            selected = selectedMode == AppThemeMode.DARK,
            onClick = { onModeSelected(AppThemeMode.DARK) }
        )
        SegmentedButton(
            icon = Icons.Default.Settings,
            label = "System",
            selected = selectedMode == AppThemeMode.SYSTEM,
            onClick = { onModeSelected(AppThemeMode.SYSTEM) }
        )
    }
}

@Composable
fun SegmentedButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) ButtonDefaults.outlinedButtonBorder else null,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun StatColumn(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}
