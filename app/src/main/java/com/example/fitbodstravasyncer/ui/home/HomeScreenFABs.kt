package com.example.fitbodstravasyncer.ui.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.fitbodstravasyncer.ui.UiConstants.FAB_PADDING
import com.example.fitbodstravasyncer.ui.UiConstants.FAB_VERTICAL_SPACING
import com.example.fitbodstravasyncer.ui.UiStrings
import com.example.fitbodstravasyncer.ui.composables.AnimatedFab
import com.example.fitbodstravasyncer.util.UiState
import com.example.fitbodstravasyncer.worker.StravaUploadWorker

@Composable
internal fun FloatingActionButtons(
    selectedIds: Set<String>,
    state: UiState,
    context: Context,
    showDelete: () -> Unit,
    setShowSheet: () -> Unit,
    viewModel: HomeViewModel,
    setLastActionToast: (String?) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(FAB_PADDING)
    ) {
        Column(
            modifier = Modifier.align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(FAB_VERTICAL_SPACING)
        ) {
            AnimatedFab(
                visible = selectedIds.isNotEmpty(),
                onClick = {
                    val selectedSessions = state.sessionMetrics.filter { selectedIds.contains(it.id) }
                    val alreadySynced = selectedSessions.filter { it.stravaId != null }
                    val toSync = selectedSessions.filter { it.stravaId == null }
                    if (alreadySynced.isNotEmpty()) {
                        setLastActionToast("Already synced")
                    }
                    if (toSync.isNotEmpty()) {
                        toSync.forEach { session ->
                            StravaUploadWorker.enqueue(context, session.id)
                        }
                        setLastActionToast("Syncing ${toSync.size} workout(s) to Strava")
                    }
                },
                icon = Icons.Default.CloudDone,
                contentDescription = UiStrings.SYNC_SELECTED,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
            AnimatedFab(
                visible = selectedIds.isNotEmpty(),
                onClick = showDelete,
                icon = Icons.Default.Delete,
                contentDescription = UiStrings.DELETE_SELECTED,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        FloatingActionButton(
            onClick = setShowSheet,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = FAB_PADDING)
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = UiStrings.MORE_ACTIONS)
        }
    }
}