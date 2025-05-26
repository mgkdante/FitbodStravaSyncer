// MainScreen.kt

package com.example.fitbodstravasyncer.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitbodstravasyncer.AppThemeMode
import com.example.fitbodstravasyncer.data.db.HeartRateSampleEntity
import com.example.fitbodstravasyncer.viewmodel.MainViewModel
import com.example.fitbodstravasyncer.viewmodel.SessionMetrics
import com.example.fitbodstravasyncer.worker.StravaUploadWorker
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs


enum class SyncFilter { ALL, NON_SYNCED, SYNCED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    appThemeMode: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }

    val selectedIds = remember { mutableStateListOf<String>() }
    var showDelete by remember { mutableStateOf(false) }
    var showDeleteAll by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }

    var syncFilter by remember { mutableStateOf(SyncFilter.ALL) }

    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fitbod → Strava") },
                actions = {
                    FilterAndThemeDropdown(
                        currentFilter = syncFilter,
                        onFilterChange = { syncFilter = it },
                        currentTheme = appThemeMode,
                        onThemeChange = onThemeChange,
                        dynamicColorEnabled     = state.dynamicColor,
                        onDynamicColorToggled   = viewModel::toggleDynamicColor
                    )
                }
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
                // Filtered list for animation
                val filteredSessions = when (syncFilter) {
                    SyncFilter.ALL -> state.sessionMetrics
                    SyncFilter.NON_SYNCED -> state.sessionMetrics.filter { it.stravaId == null }
                    SyncFilter.SYNCED -> state.sessionMetrics.filter { it.stravaId != null }
                }

                // AnimatedContent for smooth cross-fade on filter change
                AnimatedContent(
                    targetState = filteredSessions,
                    transitionSpec = {
                        fadeIn(tween(180)) togetherWith fadeOut(tween(180))
                    },
                    label = "ListAnimation"
                ) { list ->
                    if (list.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No activities fetched",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(list, key = { it.id }) { session ->
                                val expanded = expandedStates[session.id] ?: false
                                SessionCardWithCheckbox(
                                    session = session,
                                    checked = selectedIds.contains(session.id),
                                    expanded = expanded,
                                    onExpandToggle = { expandedStates[session.id] = !expanded },
                                    onCheckedChange = { checked ->
                                        if (checked) selectedIds.add(session.id)
                                        else selectedIds.remove(session.id)
                                    },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }

            if (state.isFetching) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                }
            }

            if (showDelete) {
                AlertDialog(
                    onDismissRequest = { showDelete = false },
                    title = { Text("Confirm Delete") },
                    text = { Text("Delete ${selectedIds.size} sessions?") },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.deleteSessions(selectedIds.toList())
                            selectedIds.clear()
                            showDelete = false
                        }) { Text("Yes") }
                    },
                    dismissButton = {
                        Button(onClick = { showDelete = false }) { Text("No") }
                    }
                )
            }
            if (showDeleteAll) {
                AlertDialog(
                    onDismissRequest = { showDeleteAll = false },
                    title = { Text("Confirm Delete All") },
                    text = { Text("Delete ALL sessions?") },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.deleteAllSessions()
                            selectedIds.clear()
                            showDeleteAll = false
                        }) { Text("Yes") }
                    },
                    dismissButton = {
                        Button(onClick = { showDeleteAll = false }) { Text("No") }
                    }
                )
            }

            if (showSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSheet = false },
                    sheetState = sheetState,
                    contentWindowInsets = { WindowInsets(0) }
                ) {
                    ActionsSheet(
                        state = state,
                        showDeleteAll = { showDeleteAll = true },
                        onFetch = {
                            coroutineScope.launch {
                                viewModel.fetchSessions(state.dateFrom, state.dateTo)
                            }
                            showSheet = false
                        },
                        isFetching = state.isFetching,
                        onCheckMatching = {
                            isChecking = true
                            viewModel.restoreStravaIds()
                            Toast.makeText(context, "Matching Strava workouts checked.", Toast.LENGTH_SHORT).show()
                            isChecking = false
                            showSheet = false
                        },
                        onToggleFutureSync = viewModel::toggleFutureSync,
                        onToggleDailySync = viewModel::toggleDailySync,
                        onSyncAll = {
                            viewModel.enqueueSyncAll()
                            showSheet = false
                        },
                        onDateFromChange = viewModel::setDateFrom,
                        onDateToChange = viewModel::setDateTo,
                    )
                }
            }

            // Floating Action Buttons
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Upload FAB
                    AnimatedVisibility(
                        visible = selectedIds.isNotEmpty(),
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        FloatingActionButton(
                            onClick = {
                                val selectedSessions = state.sessionMetrics.filter { selectedIds.contains(it.id) }
                                val alreadySynced = selectedSessions.filter { it.stravaId != null }
                                val toSync = selectedSessions.filter { it.stravaId == null }

                                if (alreadySynced.isNotEmpty()) {
                                    Toast.makeText(context, "Already synced", Toast.LENGTH_SHORT).show()
                                }

                                if (toSync.isNotEmpty()) {
                                    toSync.forEach { session ->
                                        StravaUploadWorker.enqueue(context, session.id)
                                    }
                                    Toast.makeText(context, "Syncing ${toSync.size} workout(s) to Strava", Toast.LENGTH_SHORT).show()
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = "Sync Selected")
                        }
                    }

                    // Trash FAB
                    AnimatedVisibility(
                        visible = selectedIds.isNotEmpty(),
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        FloatingActionButton(
                            onClick = { showDelete = true },
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                        }
                    }
                }

                // More Actions FAB at BottomEnd
                FloatingActionButton(
                    onClick = { showSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                }
            }


        }
    }
}


@Composable
fun SessionCardWithCheckbox(
    session: SessionMetrics,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer
        )
    )

    // Preprocess the dateTime string
    val rawDateTime = session.dateTime
        .replace(".", "")
        .replace("–", "-")

    val dateTimeParts = rawDateTime.split(" - ")
    val startPart = dateTimeParts.getOrNull(0) ?: ""
    val endPart = dateTimeParts.getOrNull(1) ?: ""

    val startDateTimeParts = startPart.split(" ")
    val date = startDateTimeParts.getOrNull(0) ?: ""
    val startTimeStr = startDateTimeParts.drop(1).joinToString(" ")

    val endDateTimeParts = endPart.split(" ")
    val endTimeStr = endDateTimeParts.drop(1).joinToString(" ")

    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    val startTime = try { LocalTime.parse(startTimeStr, timeFormatter) } catch (e: Exception) { null }
    val endTime = try { LocalTime.parse(endTimeStr, timeFormatter) } catch (e: Exception) { null }
    val formattedStartTime = startTime?.format(timeFormatter) ?: startTimeStr
    val formattedEndTime = endTime?.format(timeFormatter) ?: endTimeStr

    val scale by animateFloatAsState(
        targetValue = if (checked) 1.2f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "checkboxScale"
    )

    Card(
        onClick = {onExpandToggle()},
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .background(backgroundGradient)
                .padding(24.dp)
        ) {
            // Sync status icon and label at top start
            // Sync status icon, label, and checkbox on the same row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (session.stravaId != null) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Synced to Strava",
                            tint = Color(0xFF43A047),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Synced",
                            color = Color(0xFF43A047),
                            style = MaterialTheme.typography.labelMedium
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Not Synced to Strava",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Not Synced",
                            color = Color(0xFFD32F2F),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier
                        .scale(scale)
                )
            }


            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp)
            ) {
                Spacer(Modifier.height(6.dp)) // Space below icon row

                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$formattedStartTime - $formattedEndTime",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                session.description.takeIf { it.isNotBlank() }?.let { desc ->
                    Text(
                        text = desc.replace("\n", ", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatColumn(
                        icon = Icons.Default.AccessTime,
                        label = "Duration",
                        value = "${session.activeTime} min",
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                    StatColumn(
                        icon = Icons.Default.LocalFireDepartment,
                        label = "Calories",
                        value = "${session.calories.toInt()} kcal",
                        iconTint = MaterialTheme.colorScheme.error
                    )
                    session.avgHeartRate?.let {
                        StatColumn(
                            icon = Icons.Default.Favorite,
                            label = "Avg HR",
                            value = "${it.toInt()} bpm",
                            iconTint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                if (expanded && session.heartRateSeries.isNotEmpty()) {
                    HeartRateChartInteractive(session.heartRateSeries)
                }
            }
        }
    }
}


@Composable
fun HeartRateChartInteractive(
    heartRateSeries: List<HeartRateSampleEntity>
) {
    val selectedIndex = remember { mutableStateOf<Int?>(null) }

    if (heartRateSeries.size < 2) {
        Text("Not enough heart rate data for chart.")
        return
    }

    val sortedSeries = remember(heartRateSeries) { heartRateSeries.sortedBy { it.time } }

    val minTime = sortedSeries.first().time.epochSecond
    val maxTime = sortedSeries.last().time.epochSecond
    val timeRange = (maxTime - minTime).takeIf { it != 0L } ?: 1L

    val minBpm = sortedSeries.minOf { it.bpm }
    val maxBpm = sortedSeries.maxOf { it.bpm }
    val bpmRange = (maxBpm - minBpm).takeIf { it != 0L } ?: 1L

    // Axis style
    val axisColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onBackground
    val chartLineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.secondary
    val highlightColor = MaterialTheme.colorScheme.tertiary

    val yAxisLabelWidthDp = 42.dp
    val xAxisLabelHeightDp = 18.dp
    val density = LocalDensity.current
    val yAxisLabelWidthPx = with(density) { yAxisLabelWidthDp.toPx() }
    val xAxisLabelHeightPx = with(density) { xAxisLabelHeightDp.toPx() }

    // Mutable for gesture scope
    val pointerModifier = Modifier.pointerInput(sortedSeries) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val position = event.changes.firstOrNull()?.position
                if (position != null) {
                    val chartWidth = size.width - yAxisLabelWidthPx
                    val xRatio = ((position.x - yAxisLabelWidthPx).coerceIn(0f, chartWidth)) / chartWidth
                    val tappedTime = minTime + (xRatio * timeRange).toLong()
                    val idx = sortedSeries.indices.minByOrNull {
                        abs(sortedSeries[it].time.epochSecond - tappedTime)
                    }
                    selectedIndex.value = idx
                }
                // Reset tooltip when finger/mouse leaves chart area
                if (event.changes.all { !it.pressed }) {
                    // Optionally comment out the next line to keep tooltip on last touched point
                    // selectedIndex.value = null
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 8.dp)
            .then(pointerModifier)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val chartLeft = yAxisLabelWidthPx
            val chartRight = size.width
            val chartTop = 0f
            val chartBottom = size.height - xAxisLabelHeightPx
            val chartHeight = chartBottom - chartTop
            val chartWidth = chartRight - chartLeft

            // --- Axis/Label Ticks ---
            val yTicks = 4
            val xTicks = 4
            val yStep = bpmRange / yTicks
            val timeStep = timeRange / xTicks
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            // --- Draw Y Axis ---
            for (i in 0..yTicks) {
                val bpm = minBpm + (i * yStep)
                val y = chartBottom - (i.toFloat() / yTicks) * chartHeight
                drawLine(
                    color = axisColor,
                    start = Offset(chartLeft - 6f, y),
                    end = Offset(chartLeft, y),
                    strokeWidth = 2f
                )
                drawLine(
                    color = axisColor.copy(alpha = 0.15f),
                    start = Offset(chartLeft, y),
                    end = Offset(chartRight, y),
                    strokeWidth = 1f
                )
                drawContext.canvas.nativeCanvas.drawText(
                    bpm.toString(),
                    6f,
                    y + 12f,
                    android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = 32f
                        textAlign = android.graphics.Paint.Align.LEFT
                    }
                )
            }

            // --- Draw X Axis ---
            for (i in 0..xTicks) {
                val t = minTime + (i * timeStep)
                val x = chartLeft + (i.toFloat() / xTicks) * chartWidth
                drawLine(
                    color = axisColor,
                    start = Offset(x, chartBottom),
                    end = Offset(x, chartBottom + 6f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = axisColor.copy(alpha = 0.13f),
                    start = Offset(x, chartTop),
                    end = Offset(x, chartBottom),
                    strokeWidth = 1f
                )
                val label = try {
                    java.time.Instant.ofEpochSecond(t).atZone(java.time.ZoneId.systemDefault()).toLocalTime().format(timeFormatter)
                } catch (e: Exception) { "" }
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x,
                    chartBottom + xAxisLabelHeightPx,
                    android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }

            // --- Plot Line ---
            fun xFor(sample: HeartRateSampleEntity): Float =
                chartLeft + ((sample.time.epochSecond - minTime).toFloat() / timeRange) * chartWidth
            fun yFor(sample: HeartRateSampleEntity): Float =
                chartBottom - ((sample.bpm - minBpm).toFloat() / bpmRange) * chartHeight

            for (i in 1 until sortedSeries.size) {
                drawLine(
                    color = chartLineColor,
                    start = Offset(xFor(sortedSeries[i - 1]), yFor(sortedSeries[i - 1])),
                    end = Offset(xFor(sortedSeries[i]), yFor(sortedSeries[i])),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }

            // --- Draw points ---
            for (i in sortedSeries.indices) {
                val sample = sortedSeries[i]
                val selected = selectedIndex.value == i
                drawCircle(
                    color = if (selected) highlightColor else pointColor,
                    center = Offset(xFor(sample), yFor(sample)),
                    radius = if (selected) 10f else 5f
                )
            }

            // --- Highlight/Tooltip ---
            selectedIndex.value?.let { idx ->
                val sample = sortedSeries[idx]
                val x = xFor(sample)
                val y = yFor(sample)
                drawLine(
                    color = highlightColor,
                    start = Offset(x, chartTop),
                    end = Offset(x, chartBottom),
                    strokeWidth = 2f
                )
                drawCircle(highlightColor, radius = 12f, center = Offset(x, y))
            }
        }
        // Tooltip UI (below chart)
        selectedIndex.value?.let { idx ->
            val sample = sortedSeries[idx]
            val t = sample.time.atZone(java.time.ZoneId.systemDefault()).toLocalTime()
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp)
            ) {
                Text(
                    "${sample.bpm} bpm   ${t.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


@Composable
fun ActionsSheet(
    state: com.example.fitbodstravasyncer.viewmodel.UiState,
    showDeleteAll: () -> Unit,
    onFetch: () -> Unit,
    isFetching: Boolean,
    onCheckMatching: () -> Unit,
    onToggleFutureSync: (Boolean) -> Unit,
    onToggleDailySync: (Boolean) -> Unit,
    onSyncAll: () -> Unit,
    onDateFromChange: (LocalDate?) -> Unit,
    onDateToChange: (LocalDate?) -> Unit,
) {
    val dateFormatter = DateTimeFormatter.ISO_DATE
    var showDatePickerFrom by remember { mutableStateOf(false) }
    var showDatePickerTo by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Actions", style = MaterialTheme.typography.titleLarge)

        // Date From Picker
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Date From", style = MaterialTheme.typography.labelSmall)
                Text(
                    state.dateFrom?.format(dateFormatter) ?: "Not set",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            OutlinedButton(onClick = { showDatePickerFrom = true }) {
                Text("Pick")
            }
        }

        // Date To Picker
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Date To", style = MaterialTheme.typography.labelSmall)
                Text(
                    state.dateTo?.format(dateFormatter) ?: "Not set",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            OutlinedButton(onClick = { showDatePickerTo = true }) {
                Text("Pick")
            }
        }

        // Fetch Workouts Button
        LabeledButtonWithHelp(
            buttonText = "Fetch Workouts",
            onClick = onFetch,
            helpTitle = "Fetch Workouts",
            helpDescription = "Fetches Fitbod workouts between the selected dates.",
            isLoading = isFetching,
            enabled = !isFetching && state.dateFrom != null && state.dateTo != null
        )


        // Auto-sync Toggle with Help
        LabeledSwitchWithHelp(
            label = "Auto-sync 24h every 15m",
            checked = state.futureSync,
            onCheckedChange = onToggleFutureSync,
            helpTitle = "Auto-sync",
            helpDescription = "Automatically syncs workouts every 15 minutes for the past 24 hours."
        )

        // Daily Sync Toggle with Help
        LabeledSwitchWithHelp(
            label = "Daily Sync",
            checked = state.dailySync,
            onCheckedChange = onToggleDailySync,
            helpTitle = "Daily Sync",
            helpDescription = "Performs a daily synchronization of your workouts."
        )

        // Check Matching Workouts Button with Help
        LabeledButtonWithHelp(
            buttonText = "Check matching Strava workouts",
            onClick = onCheckMatching,
            helpTitle = "Check Matching Workouts",
            helpDescription = "Checks for workouts that already exist in Strava to avoid duplicates."
        )

        // Sync All Button with Help
        LabeledButtonWithHelp(
            buttonText = "Sync All",
            onClick = onSyncAll,
            helpTitle = "Sync All",
            helpDescription = "Synchronizes all workouts to Strava."
        )

        // Delete All Button with Help
        if (state.sessionMetrics.isNotEmpty()) {
            LabeledButtonWithHelp(
                buttonText = "Delete All",
                onClick = showDeleteAll,
                helpTitle = "Delete All",
                helpDescription = "Deletes all workout sessions from the list."
            )
        }
    }

    // Date Pickers
    if (showDatePickerFrom) {
        MaterialDatePickerDialog(
            initialDate = state.dateFrom ?: LocalDate.now(),
            onDateSelected = {
                onDateFromChange(it)
                showDatePickerFrom = false
            },
            onDismiss = { showDatePickerFrom = false }
        )
    }

    if (showDatePickerTo) {
        MaterialDatePickerDialog(
            initialDate = state.dateTo ?: LocalDate.now(),
            onDateSelected = {
                onDateToChange(it)
                showDatePickerTo = false
            },
            onDismiss = { showDatePickerTo = false }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val selectedDate = Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    onDateSelected(selectedDate)
                }
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun SegmentedButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) ButtonDefaults.outlinedButtonBorder else null,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun StatColumn(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}


@Composable
fun InfoDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(description) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}


@Composable
fun HelpIconButton(
    title: String,
    description: String
) {
    var showDialog by remember { mutableStateOf(false) }

    IconButton(onClick = { showDialog = true }) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Help",
            tint = MaterialTheme.colorScheme.primary
        )
    }

    if (showDialog) {
        InfoDialog(
            title = title,
            description = description,
            onDismiss = { showDialog = false }
        )
    }
}


@Composable
fun LabeledSwitchWithHelp(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    helpTitle: String,
    helpDescription: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.width(8.dp))
            HelpIconButton(title = helpTitle, description = helpDescription)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun LabeledButtonWithHelp(
    buttonText: String,
    onClick: () -> Unit,
    helpTitle: String,
    helpDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(buttonText)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = { showDialog = true }) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(helpTitle) },
            text = { Text(helpDescription) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}


@Composable
fun FilterAndThemeDropdown(
    currentFilter: SyncFilter,
    onFilterChange: (SyncFilter) -> Unit,
    currentTheme: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorToggled: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // — Filters —
            DropdownMenuItem(
                text = { Text("Filter: All") },
                onClick = {
                    onFilterChange(SyncFilter.ALL)
                    expanded = false
                },
                leadingIcon = { Icon(Icons.Default.List, contentDescription = null) },
                trailingIcon = {
                    if (currentFilter == SyncFilter.ALL) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("Filter: Non Synced") },
                onClick = {
                    onFilterChange(SyncFilter.NON_SYNCED)
                    expanded = false
                },
                leadingIcon = { Icon(Icons.Default.CloudOff, contentDescription = null) },
                trailingIcon = {
                    if (currentFilter == SyncFilter.NON_SYNCED) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("Filter: Synced") },
                onClick = {
                    onFilterChange(SyncFilter.SYNCED)
                    expanded = false
                },
                leadingIcon = { Icon(Icons.Default.CloudDone, contentDescription = null) },
                trailingIcon = {
                    if (currentFilter == SyncFilter.SYNCED) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )

            HorizontalDivider()

            // — Theme Modes —
            DropdownMenuItem(
                text = { Text("Theme: Light") },
                onClick = {
                    onThemeChange(AppThemeMode.LIGHT)
                    expanded = false
                },
                leadingIcon = { Icon(Icons.Default.Brightness7, contentDescription = null) },
                trailingIcon = {
                    if (currentTheme == AppThemeMode.LIGHT) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("Theme: Dark") },
                onClick = {
                    onThemeChange(AppThemeMode.DARK)
                    expanded = false
                },
                leadingIcon = { Icon(Icons.Default.Brightness4, contentDescription = null) },
                trailingIcon = {
                    if (currentTheme == AppThemeMode.DARK) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("Theme: System") },
                onClick = {
                    onThemeChange(AppThemeMode.SYSTEM)
                    expanded = false
                },
                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                trailingIcon = {
                    if (currentTheme == AppThemeMode.SYSTEM) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )

            HorizontalDivider()

            // — Dynamic Color Toggle —
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Use Dynamic Color")
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = dynamicColorEnabled,
                            onCheckedChange = {
                                onDynamicColorToggled(it)
                            }
                        )
                    }
                },
                onClick = { /* handled by the Switch above */ }
            )
        }
    }
}

