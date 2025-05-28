import HeartRateChartInteractive
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitbodstravasyncer.ui.UiConstants.ANIMATION_DURATION_MS
import com.example.fitbodstravasyncer.ui.UiConstants.CARD_CORNER
import com.example.fitbodstravasyncer.ui.UiConstants.CARD_ELEVATION
import com.example.fitbodstravasyncer.ui.UiConstants.CHECKBOX_SCALE_DEFAULT
import com.example.fitbodstravasyncer.ui.UiConstants.CHECKBOX_SCALE_SELECTED
import com.example.fitbodstravasyncer.util.SessionMetrics
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH) }
    val startTime = try { LocalTime.parse(startTimeStr, timeFormatter) } catch (e: Exception) { null }
    val endTime = try { LocalTime.parse(endTimeStr, timeFormatter) } catch (e: Exception) { null }
    val formattedStartTime = startTime?.format(timeFormatter) ?: startTimeStr
    val formattedEndTime = endTime?.format(timeFormatter) ?: endTimeStr

    val scale by animateFloatAsState(
        targetValue = if (checked) CHECKBOX_SCALE_SELECTED else CHECKBOX_SCALE_DEFAULT,
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS),
        label = "checkboxScale"
    )

    Card(
        onClick = { onExpandToggle() },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(CARD_CORNER),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(CARD_ELEVATION)
    ) {
        Box(
            modifier = Modifier
                .background(backgroundGradient)
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SyncStatusLabel(session.stravaId)
                    Checkbox(
                        checked = checked,
                        onCheckedChange = onCheckedChange,
                        modifier = Modifier.scale(scale)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                    // ---- Animated visibility for expanded content ----
                    AnimatedVisibility(
                        visible = expanded && session.heartRateSeries.isNotEmpty(),
                        enter = fadeIn(tween(250)) + expandVertically(),
                        exit = fadeOut(tween(250)) + shrinkVertically()
                    ) {
                        HeartRateChartInteractive(session.heartRateSeries)
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusLabel(stravaId: Long?) {
    val isSynced = stravaId != null
    val (icon, text, color) = if (isSynced) {
        Triple(Icons.Default.CloudDone, "Synced", Color(0xFF43A047))
    } else {
        Triple(Icons.Default.CloudOff, "Not Synced", Color(0xFFD32F2F))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = text, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelMedium)
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
