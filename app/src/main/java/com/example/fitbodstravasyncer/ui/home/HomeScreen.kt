package com.example.fitbodstravasyncer.ui.home

import SessionCardWithCheckbox
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.fitbodstravasyncer.ui.SyncFilter
import com.example.fitbodstravasyncer.ui.UiConstants.CARD_HORIZONTAL_PADDING
import com.example.fitbodstravasyncer.ui.UiConstants.CARD_VERTICAL_PADDING
import com.example.fitbodstravasyncer.ui.UiConstants.EMPTY_ICON_SIZE
import com.example.fitbodstravasyncer.ui.UiConstants.FAB_PADDING
import com.example.fitbodstravasyncer.ui.UiConstants.FAB_VERTICAL_SPACING
import com.example.fitbodstravasyncer.ui.composables.ActionsSheet
import com.example.fitbodstravasyncer.ui.composables.AnimatedFab
import com.example.fitbodstravasyncer.ui.composables.ConfirmDialog
import com.example.fitbodstravasyncer.ui.composables.LoadingOverlay
import com.example.fitbodstravasyncer.ui.main.AppThemeMode
import com.example.fitbodstravasyncer.worker.StravaUploadWorker
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: HomeViewModel,
    appThemeMode: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var showDeleteAll by rememberSaveable { mutableStateOf(false) }
    var isChecking by rememberSaveable { mutableStateOf(false) }
    var syncFilter by rememberSaveable { mutableStateOf(SyncFilter.ALL) }
    val selectedIds by viewModel.selectedIds.collectAsState()
    val expandedIds by viewModel.expandedIds.collectAsState()
    var lastActionToast by remember { mutableStateOf<String?>(null) }

    // Efficient filtered session computation (still memoized)
    val filteredSessions by remember(state.sessionMetrics, syncFilter) {
        derivedStateOf {
            when (syncFilter) {
                SyncFilter.ALL -> state.sessionMetrics
                SyncFilter.NON_SYNCED -> state.sessionMetrics.filter { it.stravaId == null }
                SyncFilter.SYNCED -> state.sessionMetrics.filter { it.stravaId != null }
            }
        }
    }

    // Global Toast handler
    LaunchedEffect(lastActionToast) {
        lastActionToast?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            lastActionToast = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fitbod → Strava") },
                actions = {
                    FilterAndThemeDropdown(
                        currentFilter = syncFilter,
                        onFilterChange = { syncFilter = it },
                        currentTheme = appThemeMode,
                        onThemeChange = onThemeChange,
                        dynamicColorEnabled = state.dynamicColor,
                        onDynamicColorToggled = viewModel::toggleDynamicColor
                    )
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Remove AnimatedContent - direct LazyColumn
                if (filteredSessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(CARD_HORIZONTAL_PADDING),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "No activities",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(EMPTY_ICON_SIZE)
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "No activities fetched",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(CARD_HORIZONTAL_PADDING),
                        verticalArrangement = Arrangement.spacedBy(CARD_VERTICAL_PADDING),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredSessions, key = { it.id }) { session ->
                            SessionCardWithCheckbox(
                                session = session,
                                checked = selectedIds.contains(session.id),
                                expanded = expandedIds.contains(session.id),
                                onExpandToggle = { viewModel.toggleExpansion(session.id) },
                                onCheckedChange = { viewModel.toggleSelection(session.id) },
                                // animateContentSize ONLY for expanded card, NOT on every card
                                modifier = Modifier
                                    .animateItem()
                                    .animateContentSize()

                            )
                        }
                    }
                }
            }

            // Loading overlay
            if (state.isFetching) {
                LoadingOverlay()
            }

            // Confirmation dialogs
            ConfirmDialog(
                visible = showDelete,
                title = "Confirm Delete",
                text = "Delete ${selectedIds.size} sessions?",
                onConfirm = {
                    viewModel.deleteSessions(selectedIds.toList())
                    viewModel.clearSelection()
                    showDelete = false
                    lastActionToast = "Deleted ${selectedIds.size} sessions"
                },
                onDismiss = { showDelete = false }
            )
            ConfirmDialog(
                visible = showDeleteAll,
                title = "Confirm Delete All",
                text = "Delete ALL sessions?",
                onConfirm = {
                    viewModel.deleteAllSessions()
                    viewModel.clearSelection()
                    showDeleteAll = false
                    lastActionToast = "All sessions deleted"
                },
                onDismiss = { showDeleteAll = false }
            )

            // Bottom sheet
            if (showSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSheet = false },
                    sheetState = sheetState,
                    contentWindowInsets = { WindowInsets(0) }
                ) {
                    ActionsSheet(
                        state = state,
                        showDeleteAll = { showDeleteAll = true },
                        onFetch = {
                            coroutineScope.launch {
                                viewModel.fetchSessions(state.dateFrom, state.dateTo)
                                lastActionToast = "Fetched activities"
                            }
                            showSheet = false
                        },
                        isFetching = state.isFetching,
                        isChecking = isChecking,
                        onCheckMatching = {
                            if (!isChecking) {
                                isChecking = true
                                viewModel.triggerCheckMatching {
                                    viewModel.restoreStravaIds()
                                    viewModel.unsyncIfStravaDeleted()
                                    lastActionToast = "Matching Strava workouts checked"
                                    isChecking = false
                                    showSheet = false
                                }
                            } else {
                                lastActionToast = "Please wait 15 minutes between checks."
                            }
                        }
                        ,
                        onToggleFutureSync = { viewModel.toggleFutureSync(it) },
                        onToggleDailySync = { viewModel.toggleDailySync(it) },
                        onSyncAll = {
                            viewModel.enqueueSyncAll()
                            lastActionToast = "Sync all requested"
                            showSheet = false
                        },
                        onDateFromChange = { viewModel.setDateFrom(it) },
                        onDateToChange = { viewModel.setDateTo(it) }
                    )
                }
            }

            // Floating Action Buttons
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(FAB_PADDING)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalArrangement = Arrangement.spacedBy(FAB_VERTICAL_SPACING)
                ) {
                    // Upload FAB
                    AnimatedFab(
                        visible = selectedIds.isNotEmpty(),
                        onClick = {
                            val selectedSessions = state.sessionMetrics.filter { selectedIds.contains(it.id) }
                            val alreadySynced = selectedSessions.filter { it.stravaId != null }
                            val toSync = selectedSessions.filter { it.stravaId == null }
                            if (alreadySynced.isNotEmpty()) {
                                lastActionToast = "Already synced"
                            }
                            if (toSync.isNotEmpty()) {
                                toSync.forEach { session ->
                                    StravaUploadWorker.enqueue(context, session.id)
                                }
                                lastActionToast = "Syncing ${toSync.size} workout(s) to Strava"
                            }
                        },
                        icon = Icons.Default.CloudDone,
                        contentDescription = "Sync Selected",
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )

                    // Trash FAB
                    AnimatedFab(
                        visible = selectedIds.isNotEmpty(),
                        onClick = { showDelete = true },
                        icon = Icons.Default.Delete,
                        contentDescription = "Delete Selected",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                // More Actions FAB at BottomEnd
                FloatingActionButton(
                    onClick = { showSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = FAB_PADDING)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Actions")
                }
            }
        }
    }
}



@Composable
fun FilterAndThemeDropdown(
    currentFilter: SyncFilter,
    onFilterChange: (SyncFilter) -> Unit,
    currentTheme: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorToggled: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val themes = listOf(
        Triple("Theme: Light", AppThemeMode.LIGHT, Icons.Default.Brightness7),
        Triple("Theme: Dark", AppThemeMode.DARK, Icons.Default.Brightness4),
        Triple("Theme: System", AppThemeMode.SYSTEM, Icons.Default.Settings)
    )

    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .widthIn(min = 220.dp)
                .padding(vertical = 8.dp)
        ) {
            // Filters section
            Text(
                "Filter Activities",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            SyncFilter.values().forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.label, modifier = Modifier.padding(vertical = 4.dp)) },
                    onClick = { onFilterChange(filter) }, // Do NOT close dropdown
                    leadingIcon = { Icon(filter.icon, contentDescription = null) },
                    trailingIcon = {
                        if (currentFilter == filter) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider()

            // Themes section
            Text(
                "App Theme",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            themes.forEach { (label, value, icon) ->
                DropdownMenuItem(
                    text = { Text(label, modifier = Modifier.padding(vertical = 4.dp)) },
                    onClick = { onThemeChange(value) }, // Do NOT close dropdown
                    leadingIcon = { Icon(icon, contentDescription = null) },
                    trailingIcon = {
                        if (currentTheme == value) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider()

            // Dynamic Color Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Use Dynamic Color",
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                )
                Switch(
                    checked = dynamicColorEnabled,
                    onCheckedChange = { onDynamicColorToggled(it) }
                )
            }
        }
    }
}