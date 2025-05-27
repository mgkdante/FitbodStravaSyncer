package com.example.fitbodstravasyncer.ui.auth

import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.browser.customtabs.CustomTabsIntent
import com.example.fitbodstravasyncer.auth.StravaAuthManager

fun launchStravaAuthFlow(
    stravaAuthLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val authUri = StravaAuthManager.buildAuthUri()
    val customTabs = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .build()

    stravaAuthLauncher.launch(customTabs.intent.setData(authUri))
}