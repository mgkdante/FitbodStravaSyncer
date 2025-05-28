package com.example.fitbodstravasyncer.ui.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import com.example.fitbodstravasyncer.ui.UiStrings
import com.example.fitbodstravasyncer.ui.composables.ActionsSheet
import com.example.fitbodstravasyncer.ui.composables.ConfirmDialog
import com.example.fitbodstravasyncer.util.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun ConfirmDeleteDialogs(
    showDelete: Boolean,
    setShowDelete: (Boolean) -> Unit,
    showDeleteAll: Boolean,
    setShowDeleteAll: (Boolean) -> Unit,
    selectedIds: Set<String>,
    viewModel: HomeViewModel,
    setLastActionToast: (String?) -> Unit
) {
    ConfirmDialog(
        visible = showDelete,
        title = UiStrings.CONFIRM_DELETE_TITLE,
        text = "Delete ${selectedIds.size} sessions?",
        onConfirm = {
            viewModel.deleteSessions(selectedIds.toList())
            viewModel.clearSelection()
            setShowDelete(false)
            setLastActionToast("Deleted ${selectedIds.size} sessions")
        },
        onDismiss = { setShowDelete(false) }
    )
    ConfirmDialog(
        visible = showDeleteAll,
        title = UiStrings.CONFIRM_DELETE_ALL_TITLE,
        text = UiStrings.CONFIRM_DELETE_ALL_TEXT,
        onConfirm = {
            viewModel.deleteAllSessions()
            viewModel.clearSelection()
            setShowDeleteAll(false)
            setLastActionToast("All sessions deleted")
        },
        onDismiss = { setShowDeleteAll(false) }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActionsBottomSheet(
    showSheet: () -> Unit,
    sheetState: SheetState,
    state: UiState,
    coroutineScope: CoroutineScope,
    viewModel: HomeViewModel,
    isChecking: Boolean,
    setIsChecking: (Boolean) -> Unit,
    setShowDeleteAll: (Boolean) -> Unit,
    setLastActionToast: (String?) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { showSheet() },
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        ActionsSheet(
            state = state,
            showDeleteAll = { setShowDeleteAll(true) },
            onFetch = {
                coroutineScope.launch {
                    viewModel.fetchSessions(state.dateFrom, state.dateTo)
                    setLastActionToast("Fetched activities")
                }
                showSheet()
            },
            isFetching = state.isFetching,
            isChecking = isChecking,
            onCheckMatching = {
                if (!isChecking) {
                    setIsChecking(true)
                    viewModel.triggerCheckMatching {
                        viewModel.restoreStravaIds()
                        viewModel.unsyncIfStravaDeleted()
                        setLastActionToast("Matching Strava workouts checked")
                        setIsChecking(false)
                        showSheet()
                    }
                } else {
                    setLastActionToast("Please wait 15 minutes between checks.")
                }
            },
            onToggleFutureSync = { viewModel.toggleFutureSync(it) },
            onToggleDailySync = { viewModel.toggleDailySync(it) },
            onSyncAll = {
                viewModel.enqueueSyncAll()
                setLastActionToast("Sync all requested")
                showSheet()
            },
            onDateFromChange = { viewModel.setDateFrom(it) },
            onDateToChange = { viewModel.setDateTo(it) }
        )
    }
}