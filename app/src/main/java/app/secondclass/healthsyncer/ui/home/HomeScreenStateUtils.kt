package app.secondclass.healthsyncer.ui.home

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.secondclass.healthsyncer.ui.SyncFilter
import app.secondclass.healthsyncer.util.SessionMetrics
import app.secondclass.healthsyncer.util.UiState

@Composable
internal fun rememberSortedSessions(
    state: UiState,
    syncFilter: SyncFilter,
    sessionOrder: SessionOrder
): Pair<List<SessionMetrics>, LazyListState> {
    val filteredSessions by remember(state.sessionMetrics, syncFilter) {
        derivedStateOf {
            when (syncFilter) {
                SyncFilter.ALL -> state.sessionMetrics
                SyncFilter.NON_SYNCED -> state.sessionMetrics.filter { it.stravaId == null }
                SyncFilter.SYNCED -> state.sessionMetrics.filter { it.stravaId != null }
            }
        }
    }
    val sortedSessions = remember(filteredSessions, sessionOrder) {
        when (sessionOrder) {
            SessionOrder.NEWEST_FIRST -> filteredSessions.sortedByDescending { it.startTime }
            SessionOrder.OLDEST_FIRST -> filteredSessions.sortedBy { it.startTime }
        }
    }
    val listState = rememberLazyListState()
    return Pair(sortedSessions, listState)
}


@Composable
internal fun HandleScrollRestore(
    sortedSessions: List<SessionMetrics>,
    sessionOrder: SessionOrder,
    listState: LazyListState
) {
    var prevFirstSessionId by remember { mutableStateOf(sortedSessions.firstOrNull()?.id) }
    var prevSessionsCount by remember { mutableStateOf(sortedSessions.size) }
    var prevSessionOrder by remember { mutableStateOf(sessionOrder) }

    LaunchedEffect(sortedSessions, sessionOrder) {
        val currentFirstId = sortedSessions.firstOrNull()?.id
        val newItemAddedToTop = prevSessionsCount < sortedSessions.size &&
                prevFirstSessionId != null &&
                currentFirstId != null &&
                prevFirstSessionId != currentFirstId
        val orderChanged = prevSessionOrder != sessionOrder
        if (newItemAddedToTop || orderChanged) {
            listState.animateScrollToItem(0)
        }
        prevFirstSessionId = currentFirstId
        prevSessionsCount = sortedSessions.size
        prevSessionOrder = sessionOrder
    }
}


@Composable
internal fun HandleToast(lastActionToast: String?, context: Context, onClear: () -> Unit) {
    LaunchedEffect(lastActionToast) {
        lastActionToast?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onClear()
        }
    }
}
