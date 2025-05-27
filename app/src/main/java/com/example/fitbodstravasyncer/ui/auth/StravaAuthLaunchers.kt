package com.example.fitbodstravasyncer.ui.auth

import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.example.fitbodstravasyncer.data.strava.StravaConstants.REDIRECT_URI
import kotlinx.coroutines.launch

@Composable
fun rememberStravaAuthLauncher(
    onAuthCodeReceived: (String) -> Unit
): ManagedActivityResultLauncher<Intent, ActivityResult> {
    val lifecycleScope = rememberCoroutineScope()
    return rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.data
            ?.takeIf { it.toString().startsWith(REDIRECT_URI) }
            ?.getQueryParameter("code")
            ?.let { code ->
                lifecycleScope.launch {
                    onAuthCodeReceived(code)
                }
            }
    }
}