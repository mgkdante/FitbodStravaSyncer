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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fitbodstravasyncer.ui.SyncFilter
import com.example.fitbodstravasyncer.ui.UiConstants.CARD_HORIZONTAL_PADDING
import com.example.fitbodstravasyncer.ui.UiConstants.CARD_VERTICAL_PADDING
import com.example.fitbodstravasyncer.ui.UiConstants.EMPTY_ICON_SIZE
import com.example.fitbodstravasyncer.ui.UiStrings
import com.example.fitbodstravasyncer.ui.composables.LoadingProgressIndicator
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
    viewModel: HomeViewModel,
    pendingDataLoad: Boolean,
    hasFetchedOnce: Boolean
) {
    when {
        pendingDataLoad -> {
            LoadingProgressIndicator()
        }
        sessions.isEmpty() && hasFetchedOnce -> {
            // Only show empty state after fetch attempt completed AND not pending
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "No activities",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "No activities fetched",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        sessions.isNotEmpty() -> {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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