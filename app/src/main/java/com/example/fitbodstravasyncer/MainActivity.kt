package com.example.fitbodstravasyncer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.contracts.HealthPermissionsRequestContract
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import com.example.fitbodstravasyncer.ui.MainScreen
import com.example.fitbodstravasyncer.ui.theme.FitbodStravaSyncerTheme
import com.example.fitbodstravasyncer.viewmodel.MainViewModel
import com.example.fitbodstravasyncer.worker.StravaAutoUploadWorker

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val healthConnectClient = HealthConnectClient.getOrCreate(this)

        setContent {
            var appThemeMode by rememberSaveable { mutableStateOf(AppThemeMode.SYSTEM) }
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (appThemeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.LIGHT  -> false
                AppThemeMode.DARK   -> true
            }

            val permissions: Set<String> = setOf(
                HealthPermission.getReadPermission(ExerciseSessionRecord::class),
                HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
                HealthPermission.getReadPermission(HeartRateRecord::class)
            )

            var hasPermissions by remember { mutableStateOf(false) }
            var showRequestPermissionButton by remember { mutableStateOf(false) }


            val permissionLauncher = rememberLauncherForActivityResult(
                contract = HealthPermissionsRequestContract()
            ) { granted: Set<String> ->
                if (granted.containsAll(permissions)) {
                    StravaAutoUploadWorker.schedule(this@MainActivity)
                }
            }
            LaunchedEffect(Unit) {
                hasPermissions = healthConnectClient.permissionController.getGrantedPermissions()
                    .containsAll(permissions)
                showRequestPermissionButton = !hasPermissions

                if (!hasPermissions) {
                    // Don't auto-launch, wait for user to tap button below
                } else {
                    StravaAutoUploadWorker.schedule(this@MainActivity)
                }
            }

            FitbodStravaSyncerTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (hasPermissions) {
                        MainScreen(
                            viewModel = viewModel,
                            appThemeMode = appThemeMode,
                            onThemeChange = { appThemeMode = it }
                        )
                    } else {
                        // Show UI to explain permissions & request them
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "This app requires access to your Health Connect data to sync workouts.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            if (showRequestPermissionButton) {
                                Button(onClick = {
                                    permissionLauncher.launch(permissions)
                                }) {
                                    Text("Grant Health Connect Permissions")
                                }
                            } else {
                                Text("Waiting for permission...")
                            }
                        }
                    }
                }
            }
        }
    }
}
