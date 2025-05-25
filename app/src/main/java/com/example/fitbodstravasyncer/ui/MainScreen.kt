package com.example.fitbodstravasyncer.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.fitbodstravasyncer.viewmodel.MainViewModel
import com.example.fitbodstravasyncer.viewmodel.SessionMetrics
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // DEBUG: Watch isFetching state in logcat
    LaunchedEffect(state.isFetching) {
        android.util.Log.d("SPINNER-DEBUG", "Compose sees isFetching = ${state.isFetching}")
    }

    // For selection and confirmation
    val selectedIds = remember { mutableStateListOf<String>() }
    var showDelete by remember { mutableStateOf(false) }
    var showDeleteAll by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Fitbod → Strava") }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DatePickerButton("Date From", state.dateFrom, viewModel::setDateFrom)
            DatePickerButton("Date To", state.dateTo, viewModel::setDateTo)

            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.fetchSessions(state.dateFrom, state.dateTo)
                    }
                },
                enabled = !state.isFetching && state.dateFrom != null && state.dateTo != null
            ) {
                if (state.isFetching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Fetch Workouts")
            }

            // Delete buttons row (unchanged)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (selectedIds.isNotEmpty()) {
                    Button(
                        onClick = { showDelete = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) { Text("Delete ${selectedIds.size}") }
                }
                if (state.sessionMetrics.isNotEmpty()) {
                    Button(
                        onClick = { showDeleteAll = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) { Text("Delete All") }
                }
            }

            // Confirmation dialogs (unchanged)
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

            // "Check matching Strava workouts" button (unchanged)
            Button(
                enabled = !isChecking,
                onClick = {
                    isChecking = true
                    viewModel.restoreStravaIds()
                    Toast.makeText(context, "Matching Strava workouts checked.", Toast.LENGTH_SHORT).show()
                    isChecking = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Check matching Strava workouts")
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Auto-sync 24h every 15m")
                Switch(checked = state.futureSync, onCheckedChange = viewModel::toggleFutureSync)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Daily Sync")
                Switch(checked = state.dailySync, onCheckedChange = viewModel::toggleDailySync)
            }

            Button(onClick = { viewModel.enqueueSyncAll() }, Modifier.fillMaxWidth()) {
                Text("Sync All")
            }

            // Sessions list with checkboxes for delete selection (unchanged)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.sessionMetrics) { session ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedIds.contains(session.id),
                            onCheckedChange = { checked ->
                                if (checked) selectedIds.add(session.id)
                                else selectedIds.remove(session.id)
                            }
                        )
                        SessionCard(session)
                    }
                }
            }
        }
    }
}



@Composable
fun DatePickerButton(
    label: String,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.Start) {
        if (selectedDate != null) Text(label, style = MaterialTheme.typography.labelMedium)
        Button(onClick = { showDialog = true }) {
            Text(selectedDate?.format(DateTimeFormatter.ISO_DATE) ?: "Select $label")
        }
        if (showDialog) {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                context,
                { _, y, m, d ->
                    onDateSelected(LocalDate.of(y, m + 1, d))
                    showDialog = false
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }
}

@Composable
fun SessionCard(session: SessionMetrics) {
    val bg = if (session.stravaId != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(session.title, style = MaterialTheme.typography.titleMedium)
            Text(session.dateTime, style = MaterialTheme.typography.bodySmall)
            Text("Duration: ${session.activeTime} min", style = MaterialTheme.typography.bodySmall)
            Text("Calories: ${session.calories.toInt()}", style = MaterialTheme.typography.bodySmall)
            session.avgHeartRate?.let { Text("Avg HR: ${it.toInt()} bpm", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
