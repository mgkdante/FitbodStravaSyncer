package com.example.fitbodstravasyncer.ui.main

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import com.example.fitbodstravasyncer.core.theme.FitbodStravaSyncerTheme
import com.example.fitbodstravasyncer.ui.auth.AuthScreen
import com.example.fitbodstravasyncer.ui.auth.launchStravaAuthFlow
import com.example.fitbodstravasyncer.ui.auth.rememberStravaAuthLauncher
import com.example.fitbodstravasyncer.ui.composables.LoadingProgressIndicator
import com.example.fitbodstravasyncer.ui.home.HomeViewModel
import com.example.fitbodstravasyncer.ui.home.HomeScreen
import com.example.fitbodstravasyncer.ui.util.OnResumeEffect
import com.example.fitbodstravasyncer.ui.util.PermissionAndAuthEffects
import com.example.fitbodstravasyncer.ui.util.rememberPermissionLauncher

// --- Main Content Composable (Activity logic extracted here) ---
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainActivityContent(
    viewModel: HomeViewModel,
    healthConnectClient: HealthConnectClient
) {
    // --- Theme state ---
    var appThemeMode by rememberSaveable { mutableStateOf(AppThemeMode.SYSTEM) }
    val darkTheme = when (appThemeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT  -> false
        AppThemeMode.DARK   -> true
    }

    // --- Permissions and state ---
    val permissions = remember {
        setOf(
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY,
        )
    }
    var permissionsChecked by remember { mutableStateOf(false) }
    var hasHealthPermissions by remember { mutableStateOf(false) }
    val isStravaConnected by viewModel.isStravaConnected.collectAsState()
    var refreshKey by remember { mutableStateOf(0) }

    // --- Permission launcher ---
    val permissionLauncher = rememberPermissionLauncher(
        permissions = permissions,
        onPermissionsResult = { granted ->
            hasHealthPermissions = granted.containsAll(permissions)
            refreshKey++
        }
    )

    // --- Strava Auth launcher ---
    val stravaAuthLauncher = rememberStravaAuthLauncher(
        onAuthCodeReceived = { code ->
            viewModel.launchExchangeStravaCode(code)
        }
    )

    // --- Launchers and lifecycle effects ---
    PermissionAndAuthEffects(
        healthConnectClient = healthConnectClient,
        permissions = permissions,
        setHasHealthPermissions = { hasHealthPermissions = it },
        setPermissionsChecked = { permissionsChecked = it },
        viewModel = viewModel
    )

    // --- Lifecycle observer for ON_RESUME ---
    OnResumeEffect { viewModel.updateStravaConnectionState() }

    // --- UI Theme and Scaffold ---
    FitbodStravaSyncerTheme(darkTheme = darkTheme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!permissionsChecked) {
                LoadingProgressIndicator()
            } else {
                if (!hasHealthPermissions || !isStravaConnected) {
                    AuthScreen(
                        hasHealthPermissions = hasHealthPermissions,
                        onRequestHealthPermissions = { permissionLauncher.launch(permissions) },
                        isStravaConnected = isStravaConnected,
                        onConnectStrava = {
                            launchStravaAuthFlow(stravaAuthLauncher)
                        }
                    )
                } else {
                    HomeScreen(
                        viewModel = viewModel,
                        appThemeMode = appThemeMode,
                        onThemeChange = { appThemeMode = it }
                    )
                }
            }
        }
    }
}
