package com.example.fitbodstravasyncer.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.contracts.HealthPermissionsRequestContract
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.example.fitbodstravasyncer.core.theme.FitbodStravaSyncerTheme
import com.example.fitbodstravasyncer.ui.auth.AuthScreen
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.fitbodstravasyncer.data.strava.StravaConstants.AUTH_URL
import com.example.fitbodstravasyncer.data.strava.StravaConstants.CLIENT_ID
import com.example.fitbodstravasyncer.data.strava.StravaConstants.REDIRECT_URI
import com.example.fitbodstravasyncer.ui.home.MainScreen
import com.example.fitbodstravasyncer.ui.home.HomeViewModel

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var healthConnectClient: HealthConnectClient

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        healthConnectClient = HealthConnectClient.getOrCreate(this)

        // Always update Strava connection state on launch
        viewModel.updateStravaConnectionState()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
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

            // --- Strava OAuth launcher (for in-app flow, e.g. device browserless) ---
            val stravaAuthLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                result.data?.data
                    ?.takeIf { it.toString().startsWith(REDIRECT_URI) }
                    ?.getQueryParameter("code")
                    ?.let { code ->
                        lifecycleScope.launch {
                            viewModel.exchangeStravaCodeForTokenInViewModel(code)
                            // No snackbar or toast here.
                        }
                    }
            }

            // --- Kick off Strava auth via Custom Tabs ---
            fun launchStravaAuthFlow() {
                val authUri = AUTH_URL.toUri().buildUpon()
                    .appendQueryParameter("client_id", CLIENT_ID)
                    .appendQueryParameter("redirect_uri", REDIRECT_URI)
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

            // --- Lifecycle observer for ON_RESUME ---
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.updateStravaConnectionState()
                        // No toast/snackbar here! Only after successful auth.
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // --- UI scaffold ---
            FitbodStravaSyncerTheme(darkTheme = darkTheme) {
                Surface(
                    Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!permissionsChecked) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        if (!hasHealthPermissions || !isStravaConnected) {
                            AuthScreen(
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.updateStravaConnectionState()
    }
}
