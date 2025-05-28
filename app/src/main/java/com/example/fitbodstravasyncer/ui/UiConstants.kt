package com.example.fitbodstravasyncer.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

object UiConstants {
    val CARD_HORIZONTAL_PADDING = 24.dp
    val CARD_VERTICAL_PADDING = 12.dp
    val CARD_CONTENT_PADDING = 24.dp
    val CARD_ELEVATION = 6.dp
    val CARD_CORNER = 20.dp
    const val CHECKBOX_SCALE_SELECTED = 1.2f
    const val CHECKBOX_SCALE_DEFAULT = 1f
    const val ANIMATION_DURATION_MS = 300
    const val CHART_AXIS_TICK_COUNT = 4
    val FAB_VERTICAL_SPACING = 18.dp
    val FAB_ICON_SIZE = 48.dp
    val FAB_PADDING = 16.dp
    val DIALOG_OVERLAY_ALPHA = 0.6f
    val CHART_HEIGHT = 220.dp
    val EMPTY_ICON_SIZE = 60.dp
    // Chart axis constants
    val CHART_AXIS_LABEL_X_PADDING = 6f
    val CHART_AXIS_LABEL_Y_OFFSET = 12f
    val CHART_AXIS_LABEL_TEXT_SIZE = 32f
    val CHART_X_LABEL_TEXT_SIZE = 28f
}

enum class SyncFilter(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Default.List),
    NON_SYNCED("Non Synced", Icons.Default.CloudOff),
    SYNCED(UiStrings.SYNCED, Icons.Default.CloudDone)
}