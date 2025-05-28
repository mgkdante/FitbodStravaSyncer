package com.example.fitbodstravasyncer.ui.settings

import SettingsHealthConnectSection
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.fitbodstravasyncer.util.StravaPrefs
import com.example.fitbodstravasyncer.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isStravaConnected: Boolean,
    hasHealthPermissions: Boolean, // NEW
    onRequestHealthPermissions: () -> Unit, // NEW
    onConnectStrava: (() -> Unit)? = null, // NEW
    apiUsageString: String,
    userApiWarning: Boolean,
    onDisconnectStrava: () -> Unit,
    appThemeMode: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorToggle: (Boolean) -> Unit,
    dynamicColorAvailable: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState = remember(apiUsageString, userApiWarning) {
        UiState(
            userApiUsageString = apiUsageString,
            userApiWarning = userApiWarning
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            if (StravaPrefs.isUploadCircuitBreakerTripped(context)) {
                // Show a warning banner
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        "You were logged out of Strava after repeated errors. Please reconnect to resume syncing.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            SettingsStravaSection(
                isStravaConnected = isStravaConnected,
                uiState = uiState,
                onDisconnectStrava = onDisconnectStrava,
                onConnectStrava = onConnectStrava
            )
            SettingsHealthConnectSection(
                hasHealthPermissions = hasHealthPermissions,
                onRequestHealthPermissions = onRequestHealthPermissions
            )
            SettingsThemeSection(
                appThemeMode = appThemeMode,
                onThemeChange = onThemeChange,
                dynamicColorEnabled = dynamicColorEnabled,
                onDynamicColorToggle = onDynamicColorToggle,
                dynamicColorAvailable = dynamicColorAvailable
            )
        }
    }
}
