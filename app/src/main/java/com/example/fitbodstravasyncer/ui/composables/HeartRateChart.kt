import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitbodstravasyncer.data.db.HeartRateSampleEntity
import com.example.fitbodstravasyncer.ui.UiConstants.CHART_AXIS_LABEL_TEXT_SIZE
import com.example.fitbodstravasyncer.ui.UiConstants.CHART_AXIS_LABEL_X_PADDING
import com.example.fitbodstravasyncer.ui.UiConstants.CHART_AXIS_LABEL_Y_OFFSET
import com.example.fitbodstravasyncer.ui.UiConstants.CHART_AXIS_TICK_COUNT
import com.example.fitbodstravasyncer.ui.UiConstants.CHART_HEIGHT
import com.example.fitbodstravasyncer.ui.UiConstants.CHART_X_LABEL_TEXT_SIZE
import com.example.fitbodstravasyncer.ui.UiStrings
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun HeartRateChartInteractive(
    heartRateSeries: List<HeartRateSampleEntity>
) {
    val selectedIndex = remember { mutableStateOf<Int?>(null) }

    if (heartRateSeries.size < 2) {
        Text(UiStrings.NOT_ENOUGH_HR)
        return
    }

    val sortedSeries = remember(heartRateSeries) { heartRateSeries.sortedBy { it.time } }
    val minTime = sortedSeries.first().time.epochSecond
    val maxTime = sortedSeries.last().time.epochSecond
    val timeRange = (maxTime - minTime).takeIf { it != 0L } ?: 1L

    val minBpm = sortedSeries.minOf { it.bpm }.toInt()
    val maxBpm = sortedSeries.maxOf { it.bpm }.toInt()
    val bpmRange = (maxBpm - minBpm).takeIf { it != 0 } ?: 1


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
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT)
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

            // --- Draw axes and ticks using helpers ---
            drawYAxisTicks(
                chartLeft = chartLeft,
                chartRight = chartRight,
                chartBottom = chartBottom,
                chartHeight = chartHeight,
                minBpm = minBpm,
                bpmRange = bpmRange,
                axisColor = axisColor,
                labelColor = labelColor,
                tickCount = CHART_AXIS_TICK_COUNT
            )
            drawXAxisTicks(
                chartLeft = chartLeft,
                chartBottom = chartBottom,
                chartTop = chartTop,
                chartWidth = chartWidth,
                minTime = minTime,
                timeRange = timeRange,
                axisColor = axisColor,
                labelColor = labelColor,
                tickCount = CHART_AXIS_TICK_COUNT,
                xAxisLabelHeightPx = xAxisLabelHeightPx
            )

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
            val t = sample.time.atZone(ZoneId.systemDefault()).toLocalTime()
            HeartRateChartTooltip(
                modifier = Modifier.align(Alignment.BottomCenter),
                text = "${sample.bpm} bpm   ${t.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            )

        }
    }
}

// --- DRY Helper for Y Axis ---
private fun DrawScope.drawYAxisTicks(
    chartLeft: Float,
    chartRight: Float,
    chartBottom: Float,
    chartHeight: Float,
    minBpm: Int,
    bpmRange: Int,
    axisColor: Color,
    labelColor: Color,
    tickCount: Int
) {
    val yStep = bpmRange / tickCount
    for (i in 0..tickCount) {
        val bpm = minBpm + (i * yStep)
        val y = chartBottom - (i.toFloat() / tickCount) * chartHeight
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
            CHART_AXIS_LABEL_X_PADDING,
            y + CHART_AXIS_LABEL_Y_OFFSET,
            Paint().apply {
                color = labelColor.toArgb()
                textSize = CHART_AXIS_LABEL_TEXT_SIZE
                textAlign = Paint.Align.LEFT
            }
        )
    }
}

// --- DRY Helper for X Axis ---
private fun DrawScope.drawXAxisTicks(
    chartLeft: Float,
    chartBottom: Float,
    chartTop: Float,
    chartWidth: Float,
    minTime: Long,
    timeRange: Long,
    axisColor: Color,
    labelColor: Color,
    tickCount: Int,
    xAxisLabelHeightPx: Float
) {
    val timeStep = timeRange / tickCount
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    for (i in 0..tickCount) {
        val t = minTime + (i * timeStep)
        val x = chartLeft + (i.toFloat() / tickCount) * chartWidth
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
            Instant.ofEpochSecond(t).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFormatter)
        } catch (e: Exception) { "" }
        drawContext.canvas.nativeCanvas.drawText(
            label,
            x,
            chartBottom + xAxisLabelHeightPx,
            Paint().apply {
                color = labelColor.toArgb()
                textSize = CHART_X_LABEL_TEXT_SIZE
                textAlign = Paint.Align.CENTER
            }
        )
    }
}

// --- Tooltip Helper (optional) ---
@Composable
private fun HeartRateChartTooltip(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
    ) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}
