package com.example.fitbodstravasyncer.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.fitbodstravasyncer.ui.home.HomeViewModel

@Composable
fun PermissionAndAuthEffects(
    healthConnectClient: HealthConnectClient,
    permissions: Set<String>,
    setHasHealthPermissions: (Boolean) -> Unit,
    setPermissionsChecked: (Boolean) -> Unit,
    viewModel: HomeViewModel
) {
    LaunchedEffect(true) {
        setHasHealthPermissions(
            healthConnectClient.permissionController
                .getGrantedPermissions()
                .containsAll(permissions)
        )
        setPermissionsChecked(true)
        viewModel.updateStravaConnectionState()
    }
}


@Composable
fun OnResumeEffect(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}