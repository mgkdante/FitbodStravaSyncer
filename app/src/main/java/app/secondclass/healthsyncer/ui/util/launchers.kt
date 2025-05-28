package app.secondclass.healthsyncer.ui.util

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.health.connect.client.contracts.HealthPermissionsRequestContract

@Composable
fun rememberPermissionLauncher(
    permissions: Set<String>,
    onPermissionsResult: (Set<String>) -> Unit
): ManagedActivityResultLauncher<Set<String>, Set<String>> {
    return rememberLauncherForActivityResult(
        HealthPermissionsRequestContract(),
        onPermissionsResult
    )
}