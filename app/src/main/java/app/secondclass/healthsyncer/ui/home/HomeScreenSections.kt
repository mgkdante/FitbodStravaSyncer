package app.secondclass.healthsyncer.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
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
import app.secondclass.healthsyncer.ui.SyncFilter
import app.secondclass.healthsyncer.util.SessionMetrics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopBar(
    syncFilter: SyncFilter,
    onFilterChange: (SyncFilter) -> Unit,
    sessionOrder: SessionOrder,
    onOrderChange: (SessionOrder) -> Unit,
    onSettingsClick: () -> Unit // <--- Add this!
) {
    TopAppBar(
        title = { Text(UiStrings.APP_BAR_TITLE) },
        actions = {
            // Settings gear icon
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = UiStrings.SETTINGS_TITLE)
            }
            // Your existing filter/sort menu
            FilterSortDropdown(
                currentFilter = syncFilter,
                onFilterChange = onFilterChange,
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
            ) { Text(UiStrings.CHECK_ALL) }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onUncheckAll,
                enabled = selectedIds.isNotEmpty()
            ) { Text(UiStrings.UNCHECK_ALL) }
        }
    }
}

// Three dots menu with theme/filter/sort all together
@Composable
fun FilterSortDropdown(
    currentFilter: SyncFilter,
    onFilterChange: (SyncFilter) -> Unit,
    sessionOrder: SessionOrder,
    onOrderChange: (SessionOrder) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
                    onClick = {
                        onFilterChange(filter)
                        expanded = false    // <---- CLOSE MENU after selecting
                    },
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
            Spacer(Modifier.height(10.dp))
            // Sort by Date section
            Text(
                UiStrings.SORT,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            DropdownMenuItem(
                text = { Text(if (sessionOrder == SessionOrder.NEWEST_FIRST) UiStrings.NEWEST_FIRST else UiStrings.OLDEST_FIRST) },
                onClick = {
                    onOrderChange(sessionOrder.toggle())
                    expanded = false // <---- CLOSE MENU after selecting
                },
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
        }
    }
}