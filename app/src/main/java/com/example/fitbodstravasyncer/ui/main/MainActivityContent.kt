import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.fitbodstravasyncer.core.theme.FitbodStravaSyncerTheme
import com.example.fitbodstravasyncer.ui.auth.AuthScreen
import com.example.fitbodstravasyncer.ui.auth.launchStravaAuthFlow
import com.example.fitbodstravasyncer.ui.auth.rememberStravaAuthLauncher
import com.example.fitbodstravasyncer.ui.composables.LoadingProgressIndicator
import com.example.fitbodstravasyncer.ui.home.HomeScreen
import com.example.fitbodstravasyncer.ui.home.HomeViewModel
import com.example.fitbodstravasyncer.ui.main.launchExchangeStravaCode
import com.example.fitbodstravasyncer.ui.settings.AppThemeMode
import com.example.fitbodstravasyncer.ui.settings.SettingsScreen
import com.example.fitbodstravasyncer.ui.util.OnResumeEffect
import com.example.fitbodstravasyncer.ui.util.PermissionAndAuthEffects
import com.example.fitbodstravasyncer.ui.util.rememberPermissionLauncher


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivityContent(
    viewModel: HomeViewModel,
    healthConnectClient: HealthConnectClient
) {
    // --- Navigation setup ---
    val navController = rememberNavController()
    val settingsViewModel: SettingsViewModel = viewModel()

    // --- Settings state (collect ONCE here, not per-composable!) ---
    val appThemeMode by settingsViewModel.appThemeMode.collectAsState()
    val isStravaConnected by settingsViewModel.isStravaConnected.collectAsState()
    val apiUsageString by settingsViewModel.apiUsageString.collectAsState()
    val userApiWarning by settingsViewModel.userApiWarning.collectAsState()

    // --- Compute darkTheme based on appThemeMode ---
    val darkTheme = when (appThemeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    // --- Permissions and state ---
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
    var refreshKey by remember { mutableStateOf(0) }

    val dynamicColorEnabled by settingsViewModel.dynamicColorEnabled.collectAsState()
    val dynamicColorAvailable = remember { android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S }

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
            settingsViewModel.refreshState()
        }
    )

    // --- Effects ---
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

    FitbodStravaSyncerTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColorEnabled && dynamicColorAvailable) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!permissionsChecked) {
                LoadingProgressIndicator()
            } else if (!hasHealthPermissions || !isStravaConnected) {
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
