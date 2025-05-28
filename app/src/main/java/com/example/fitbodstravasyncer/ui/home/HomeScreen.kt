package com.example.fitbodstravasyncer.ui.home

import SessionCardWithCheckbox
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.fitbodstravasyncer.ui.SyncFilter
import com.example.fitbodstravasyncer.ui.UiStrings
import com.example.fitbodstravasyncer.ui.composables.ErrorMessage
import com.example.fitbodstravasyncer.ui.composables.LoadingOverlay

enum class SessionOrder { NEWEST_FIRST, OLDEST_FIRST }
fun SessionOrder.toggle() = if (this == SessionOrder.NEWEST_FIRST) SessionOrder.OLDEST_FIRST else SessionOrder.NEWEST_FIRST

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSettingsClick: () -> Unit
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

    val sessionsUiState by viewModel.sessionsUiState.collectAsState()

    // You can remove this if you only want to show what is in the sessionsUiState
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
                sessionOrder = sessionOrder,
                onOrderChange = { sessionOrder = it },
                onSettingsClick = onSettingsClick
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (sessionsUiState) {
                is HomeViewModel.SessionsUiState.Loading -> {
                    LoadingOverlay()
                }
                is HomeViewModel.SessionsUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = UiStrings.NO_ACTIVITIES_ICON_DESC,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = UiStrings.NO_ACTIVITIES,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is HomeViewModel.SessionsUiState.Content -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CheckUncheckAllRow(
                            sessions = sortedSessions,
                            selectedIds = selectedIds,
                            onCheckAll = { viewModel.selectAll(sortedSessions.map { it.id }) },
                            onUncheckAll = { viewModel.clearSelection() }
                        )
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(sortedSessions, key = { it.id }) { session ->
                                SessionCardWithCheckbox(
                                    session = session,
                                    checked = selectedIds.contains(session.id),
                                    expanded = expandedIds.contains(session.id),
                                    onExpandToggle = { viewModel.toggleExpansion(session.id) },
                                    onCheckedChange = { viewModel.toggleSelection(session.id) },
                                    modifier = Modifier.animateContentSize()
                                )
                            }
                        }
                    }
                }
                is HomeViewModel.SessionsUiState.Error -> {
                    val message = (sessionsUiState as HomeViewModel.SessionsUiState.Error).message
                    ErrorMessage(message)
                }
            }


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
