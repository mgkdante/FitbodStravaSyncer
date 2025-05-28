package com.example.fitbodstravasyncer.ui.home

import SessionCardWithCheckbox
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fitbodstravasyncer.ui.SyncFilter
import com.example.fitbodstravasyncer.ui.UiConstants.CARD_HORIZONTAL_PADDING
import com.example.fitbodstravasyncer.ui.UiConstants.CARD_VERTICAL_PADDING
import com.example.fitbodstravasyncer.ui.UiConstants.EMPTY_ICON_SIZE
import com.example.fitbodstravasyncer.ui.UiStrings
import com.example.fitbodstravasyncer.ui.main.AppThemeMode
import com.example.fitbodstravasyncer.util.SessionMetrics



@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopBar(
    syncFilter: SyncFilter,
    onFilterChange: (SyncFilter) -> Unit,
    appThemeMode: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorToggled: (Boolean) -> Unit,
    sessionOrder: SessionOrder,
    onOrderChange: (SessionOrder) -> Unit
) {
    TopAppBar(
        title = { Text(UiStrings.APP_BAR_TITLE) },
        actions = {
            FilterThemeSortDropdown(
                currentFilter = syncFilter,
                onFilterChange = onFilterChange,
                currentTheme = appThemeMode,
                onThemeChange = onThemeChange,
                dynamicColorEnabled = dynamicColorEnabled,
                onDynamicColorToggled = onDynamicColorToggled,
                sessionOrder = sessionOrder,
                onOrderChange = onOrderChange
            )
        }
    )
}

@Composable
internal fun CheckUncheckAllRow(
    sessions: List<SessionMetrics>,
    selectedIds: Set<String>,
    onCheckAll: () -> Unit,
    onUncheckAll: () -> Unit
) {
    if (sessions.isNotEmpty()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Button(
                onClick = onCheckAll,
                enabled = sessions.any { !selectedIds.contains(it.id) }
            ) { Text("Check All") }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onUncheckAll,
                enabled = selectedIds.isNotEmpty()
            ) { Text("Uncheck All") }
        }
    }
}

@Composable
internal fun SessionsListOrEmptyState(
    sessions: List<SessionMetrics>,
    listState: LazyListState,
    selectedIds: Set<String>,
    expandedIds: Set<String>,
    viewModel: HomeViewModel
) {
    if (sessions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(CARD_HORIZONTAL_PADDING),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = UiStrings.NO_ACTIVITIES_ICON_DESC,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(EMPTY_ICON_SIZE)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = UiStrings.NO_ACTIVITIES,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(CARD_HORIZONTAL_PADDING),
            verticalArrangement = Arrangement.spacedBy(CARD_VERTICAL_PADDING),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sessions, key = { it.id }) { session ->
                SessionCardWithCheckbox(
                    session = session,
                    checked = selectedIds.contains(session.id),
                    expanded = expandedIds.contains(session.id),
                    onExpandToggle = { viewModel.toggleExpansion(session.id) },
                    onCheckedChange = { viewModel.toggleSelection(session.id) },
                    modifier = Modifier
                        .animateItem()
                        .animateContentSize()
                )
            }
        }
    }
}


