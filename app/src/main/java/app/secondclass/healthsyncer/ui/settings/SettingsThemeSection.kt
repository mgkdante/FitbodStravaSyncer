package app.secondclass.healthsyncer.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsThemeSection(
    appThemeMode: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorToggle: (Boolean) -> Unit,
    dynamicColorAvailable: Boolean
) {
    Card(
        shape = RoundedCornerShape(32.dp), // match Strava Section
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = UiStrings.APPEARANCE_TITLE,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            // Theme options, vertical, minimal padding
            AppThemeMode.entries.forEach { mode ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = appThemeMode == mode,
                        onClick = { onThemeChange(mode) }
                    )
                    Text(
                        text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 6.dp) // just a small gap
                    )
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.13f)
            )
            // Dynamic color toggle row, with just a bit more thumb room
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    UiStrings.MATERIAL_YOU_DYNAMIC_COLOR,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (dynamicColorAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = dynamicColorEnabled && dynamicColorAvailable,
                    onCheckedChange = {
                        if (dynamicColorAvailable) onDynamicColorToggle(!dynamicColorEnabled)
                    },
                    enabled = dynamicColorAvailable
                )
            }
            if (dynamicColorEnabled && dynamicColorAvailable) {
                Row(Modifier.fillMaxWidth()) {
                    AssistChip(
                        onClick = {},
                        label = { Text(UiStrings.MATERIAL_YOU_ACTIVE) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        }
    }
}
