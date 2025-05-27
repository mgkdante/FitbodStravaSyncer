package com.example.fitbodstravasyncer

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.contracts.HealthPermissionsRequestContract
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.lifecycle.lifecycleScope
import com.example.fitbodstravasyncer.data.strava.StravaAuthService
import com.example.fitbodstravasyncer.ui.MainScreen
import com.example.fitbodstravasyncer.ui.theme.FitbodStravaSyncerTheme
import com.example.fitbodstravasyncer.util.StravaPrefs
import com.example.fitbodstravasyncer.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var healthConnectClient: HealthConnectClient

    // Strava OAuth constants
    private val STRAVA_CLIENT_ID     = BuildConfig.STRAVA_CLIENT_ID
    private val STRAVA_CLIENT_SECRET = BuildConfig.STRAVA_CLIENT_SECRET
    private val STRAVA_REDIRECT_URI  = "myapp://strava-auth"
    private val STRAVA_AUTH_URL      = "https://www.strava.com/oauth/authorize"

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        healthConnectClient = HealthConnectClient.getOrCreate(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        setContent {
            // --- Theme state ---
            var appThemeMode by rememberSaveable { mutableStateOf(AppThemeMode.SYSTEM) }
            val darkTheme = when (appThemeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT  -> false
                AppThemeMode.DARK   -> true
            }
            val toolbarColor = MaterialTheme.colorScheme.primary.toArgb()

            // --- Permissions & connection state ---
            val permissions = setOf(
                HealthPermission.getReadPermission(ExerciseSessionRecord::class),
                HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
                HealthPermission.getReadPermission(HeartRateRecord::class),
                HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY,
            )

            var permissionsChecked by remember { mutableStateOf(false) }
            var hasHealthPermissions by remember { mutableStateOf(false) }
            val isStravaConnected by viewModel.isStravaConnected.collectAsState()
            var refreshKey by remember { mutableStateOf(0) }

            // --- Health Connect permission launcher ---
            val permissionLauncher = rememberLauncherForActivityResult(
                HealthPermissionsRequestContract()
            ) { granted ->
                hasHealthPermissions = granted.containsAll(permissions)
                refreshKey++
            }

            // --- Strava OAuth launcher ---
            val stravaAuthLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                result.data?.data
                    ?.takeIf { it.toString().startsWith(STRAVA_REDIRECT_URI) }
                    ?.getQueryParameter("code")
                    ?.let { code ->
                        lifecycleScope.launch {
                            viewModel.exchangeStravaCodeForTokenInViewModel(code)
                        }
                    }
            }

            // --- Kick off Strava auth via Custom Tabs ---
            fun launchStravaAuthFlow() {
                val authUri = STRAVA_AUTH_URL.toUri().buildUpon()
                    .appendQueryParameter("client_id", STRAVA_CLIENT_ID)
                    .appendQueryParameter("redirect_uri", STRAVA_REDIRECT_URI)
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("scope", "activity:read_all,activity:write")
                    .appendQueryParameter("approval_prompt", "auto")
                    .build()

                val customTabs = CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setToolbarColor(toolbarColor)
                    .build()

                stravaAuthLauncher.launch(customTabs.intent.setData(authUri))
            }

            // --- Check permissions on first composition ---
            LaunchedEffect(true) {
                hasHealthPermissions = healthConnectClient.permissionController
                    .getGrantedPermissions()
                    .containsAll(permissions)
                permissionsChecked = true
                viewModel.updateStravaConnectionState()
            }

            // --- UI scaffold ---
            FitbodStravaSyncerTheme(darkTheme = darkTheme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (!permissionsChecked) {
                        // Show a loading spinner while checking permissions
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        if (!hasHealthPermissions || !isStravaConnected) {
                            SetupScreen(
                                hasHealthPermissions = hasHealthPermissions,
                                onRequestHealthPermissions = { permissionLauncher.launch(permissions) },
                                isStravaConnected = isStravaConnected,
                                onConnectStrava = { launchStravaAuthFlow() }
                            )
                        } else {
                            MainScreen(
                                viewModel = viewModel,
                                appThemeMode = appThemeMode,
                                onThemeChange = { appThemeMode = it }
                            )
                        }
                    }
                }
            }
        }

    }
}


/** Simple two-step onboarding UI */
@Composable
fun SetupScreen(
    hasHealthPermissions: Boolean,
    onRequestHealthPermissions: () -> Unit,
    isStravaConnected: Boolean,
    onConnectStrava: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Text("Welcome! Let’s get set up.", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))

        if (!hasHealthPermissions) {
            Text("Step 1: Grant Health Connect Permission")
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRequestHealthPermissions) {
                Text("Grant Health Connect")
            }
        } else {
            Text("✔️ Health Connect Granted")
        }

        Spacer(Modifier.height(24.dp))

        if (!isStravaConnected) {
            Text("Step 2: Connect to Strava")
            Spacer(Modifier.height(8.dp))
            Button(onClick = onConnectStrava) {
                Text("Connect Strava")
            }
        } else {
            Text("✔️ Strava Connected")
        }

        Spacer(Modifier.height(32.dp))
        if (hasHealthPermissions && isStravaConnected) {
            Text("Setup complete! Entering app…")
        }
    }
}
