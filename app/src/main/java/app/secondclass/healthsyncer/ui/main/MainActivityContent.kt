// MainActivityContent.kt

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import app.secondclass.healthsyncer.core.theme.HealthSyncerTheme
import app.secondclass.healthsyncer.ui.auth.AuthScreen
import app.secondclass.healthsyncer.ui.auth.launchStravaAuthFlow
import app.secondclass.healthsyncer.ui.auth.rememberStravaAuthLauncher
import app.secondclass.healthsyncer.ui.composables.LoadingProgressIndicator
import app.secondclass.healthsyncer.ui.home.HomeScreen
import app.secondclass.healthsyncer.ui.home.HomeViewModel
import app.secondclass.healthsyncer.ui.main.launchExchangeStravaCode
import app.secondclass.healthsyncer.ui.settings.AppThemeMode
import app.secondclass.healthsyncer.ui.settings.SettingsScreen
import app.secondclass.healthsyncer.ui.util.OnResumeEffect
import app.secondclass.healthsyncer.ui.util.PermissionAndAuthEffects
import app.secondclass.healthsyncer.ui.util.rememberPermissionLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivityContent(
    viewModel: HomeViewModel,
    healthConnectClient: HealthConnectClient
) {
    val navController = rememberNavController()
    val settingsViewModel: SettingsViewModel = viewModel()

    val appThemeMode by settingsViewModel.appThemeMode.collectAsState()
    val isStravaConnected by settingsViewModel.isStravaConnected.collectAsState()
    val apiUsageString by settingsViewModel.apiUsageString.collectAsState()
    val userApiWarning by settingsViewModel.userApiWarning.collectAsState()

    val darkTheme = when (appThemeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val permissions = remember {
        setOf(
            androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                androidx.health.connect.client.records.ExerciseSessionRecord::class
            ),
            androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                androidx.health.connect.client.records.ActiveCaloriesBurnedRecord::class
            ),
            androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                androidx.health.connect.client.records.HeartRateRecord::class
            ),
            androidx.health.connect.client.permission.HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY,
        )
    }
    var permissionsChecked by remember { mutableStateOf(false) }
    var hasHealthPermissions by remember { mutableStateOf(false) }

    val dynamicColorEnabled by settingsViewModel.dynamicColorEnabled.collectAsState()
    val dynamicColorAvailable = remember { android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S }

    val permissionLauncher = rememberPermissionLauncher(
        permissions = permissions,
        onPermissionsResult = { granted ->
            hasHealthPermissions = granted.containsAll(permissions)
        }
    )

    val stravaAuthLauncher = rememberStravaAuthLauncher(
        onAuthCodeReceived = { code ->
            viewModel.launchExchangeStravaCode(code)
            settingsViewModel.refreshState()
        }
    )

    PermissionAndAuthEffects(
        healthConnectClient = healthConnectClient,
        permissions = permissions,
        setHasHealthPermissions = { hasHealthPermissions = it },
        setPermissionsChecked = { permissionsChecked = it },
        viewModel = viewModel
    )
    OnResumeEffect {
        viewModel.updateStravaConnectionState()
        settingsViewModel.refreshState()
    }

    // NEW: track if local sessions exist
    val hasLocalSessions by viewModel.hasLocalSessions.collectAsState()

    HealthSyncerTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColorEnabled && dynamicColorAvailable
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!permissionsChecked) {
                LoadingProgressIndicator()
            }
            // --- UPDATED LOGIC: Only show AuthScreen if BOTH missing AND no local data ---
            else if ((!hasHealthPermissions || !isStravaConnected) && !hasLocalSessions) {
                AuthScreen(
                    hasHealthPermissions = hasHealthPermissions,
                    onRequestHealthPermissions = { permissionLauncher.launch(permissions) },
                    isStravaConnected = isStravaConnected,
                    onConnectStrava = { launchStravaAuthFlow(stravaAuthLauncher) }
                )
            } else {
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            viewModel = viewModel,
                            onSettingsClick = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            isStravaConnected = isStravaConnected,
                            hasHealthPermissions = hasHealthPermissions,
                            onRequestHealthPermissions = { permissionLauncher.launch(permissions) },
                            onConnectStrava = { launchStravaAuthFlow(stravaAuthLauncher) },
                            apiUsageString = apiUsageString,
                            userApiWarning = userApiWarning,
                            appThemeMode = appThemeMode,
                            onThemeChange = { settingsViewModel.setAppThemeMode(it) },
                            dynamicColorEnabled = dynamicColorEnabled,
                            onDynamicColorToggle = { settingsViewModel.setDynamicColorEnabled(it) },
                            dynamicColorAvailable = dynamicColorAvailable,
                            onDisconnectStrava = { settingsViewModel.disconnectStrava() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
