package com.example.fitbodstravasyncer.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fitbodstravasyncer.ui.UiConstants.DIALOG_OVERLAY_ALPHA
import com.example.fitbodstravasyncer.ui.UiConstants.FAB_ICON_SIZE

@Composable
fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = DIALOG_OVERLAY_ALPHA)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(FAB_ICON_SIZE),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
    }
}