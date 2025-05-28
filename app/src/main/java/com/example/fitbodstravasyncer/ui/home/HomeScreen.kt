package com.example.fitbodstravasyncer.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.fitbodstravasyncer.ui.SyncFilter
import com.example.fitbodstravasyncer.ui.UiStrings
import com.example.fitbodstravasyncer.ui.composables.LoadingOverlay
import com.example.fitbodstravasyncer.ui.main.AppThemeMode
enum class SessionOrder { NEWEST_FIRST, OLDEST_FIRST }
fun SessionOrder.toggle() = if (this == SessionOrder.NEWEST_FIRST) SessionOrder.OLDEST_FIRST else SessionOrder.NEWEST_FIRST

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
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
    var sessionOrder by rememberSaveable { mutableStateOf(SessionOrder.NEWEST_FIRST) }
    val selectedIds by viewModel.selectedIds.collectAsState()
    val expandedIds by viewModel.expandedIds.collectAsState()
    var lastActionToast by remember { mutableStateOf<String?>(null) }

    val (sortedSessions, listState) = rememberSortedSessions(
        state = state,
        syncFilter = syncFilter,
        sessionOrder = sessionOrder
    )

    HandleScrollRestore(sortedSessions, sessionOrder, listState)
    HandleToast(lastActionToast, context) { lastActionToast = null }

    Scaffold(
        topBar = {
            TopBar(
                syncFilter = syncFilter,
                onFilterChange = { syncFilter = it },
                appThemeMode = appThemeMode,
                onThemeChange = onThemeChange,
                dynamicColorEnabled = state.dynamicColor,
                onDynamicColorToggled = viewModel::toggleDynamicColor,
                sessionOrder = sessionOrder,
                onOrderChange = { sessionOrder = it }
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
                CheckUncheckAllRow(
                    sessions = sortedSessions,
                    selectedIds = selectedIds,
                    onCheckAll = { viewModel.selectAll(sortedSessions.map { it.id }) },
                    onUncheckAll = { viewModel.clearSelection() }
                )
                SessionsListOrEmptyState(
                    sessions = sortedSessions,
                    listState = listState,
                    selectedIds = selectedIds,
                    expandedIds = expandedIds,
                    viewModel = viewModel
                )
            }

            if (state.isFetching) LoadingOverlay()

            ConfirmDeleteDialogs(
                showDelete = showDelete,
                setShowDelete = { showDelete = it },
                showDeleteAll = showDeleteAll,
                setShowDeleteAll = { showDeleteAll = it },
                selectedIds = selectedIds,
                viewModel = viewModel,
                setLastActionToast = { lastActionToast = it }
            )

            if (showSheet) {
                ActionsBottomSheet(
                    showSheet = { showSheet = false },
                    sheetState = sheetState,
                    state = state,
                    coroutineScope = coroutineScope,
                    viewModel = viewModel,
                    isChecking = isChecking,
                    setIsChecking = { isChecking = it },
                    setShowDeleteAll = { showDeleteAll = it },
                    setLastActionToast = { lastActionToast = it }
                )
            }

            FloatingActionButtons(
                selectedIds = selectedIds,
                state = state,
                context = context,
                showDelete = { showDelete = true },
                setShowSheet = { showSheet = true },
                viewModel = viewModel,
                setLastActionToast = { lastActionToast = it }
            )
        }
    }
}

// Three dots menu with theme/filter/sort all together
@Composable
fun FilterThemeSortDropdown(
    currentFilter: SyncFilter,
    onFilterChange: (SyncFilter) -> Unit,
    currentTheme: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorToggled: (Boolean) -> Unit,
    sessionOrder: SessionOrder,
    onOrderChange: (SessionOrder) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val themes = listOf(
        Triple(UiStrings.THEME_LIGHT, AppThemeMode.LIGHT, Icons.Default.Brightness7),
        Triple(UiStrings.THEME_DARK, AppThemeMode.DARK, Icons.Default.Brightness4),
        Triple(UiStrings.THEME_SYSTEM, AppThemeMode.SYSTEM, Icons.Default.Settings)
    )

    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = UiStrings.MENU)
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
                UiStrings.FILTER_ACTIVITIES,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            SyncFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.label, modifier = Modifier.padding(vertical = 4.dp)) },
                    onClick = { onFilterChange(filter) }, // Do NOT close dropdown
                    leadingIcon = { Icon(filter.icon, contentDescription = null) },
                    trailingIcon = {
                        if (currentFilter == filter) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = UiStrings.SELECTED,
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
                UiStrings.APP_THEME,
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
                                contentDescription = UiStrings.SELECTED,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider()

            // Sort by Date section
            Text(
                "Sort",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            DropdownMenuItem(
                text = { Text(if (sessionOrder == SessionOrder.NEWEST_FIRST) "Newest First" else "Oldest First") },
                onClick = { onOrderChange(sessionOrder.toggle()) },
                leadingIcon = {
                    Icon(
                        if (sessionOrder == SessionOrder.NEWEST_FIRST)
                            Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null
                    )
                },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

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
                    UiStrings.USE_DYNAMIC_COLOR,
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