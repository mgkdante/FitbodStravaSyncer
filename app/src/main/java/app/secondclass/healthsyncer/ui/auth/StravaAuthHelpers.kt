package app.secondclass.healthsyncer.ui.auth

import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.browser.customtabs.CustomTabsIntent
import app.secondclass.healthsyncer.auth.StravaAuthManager

fun launchStravaAuthFlow(
    stravaAuthLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val authUri = StravaAuthManager.buildAuthUri()
    val customTabs = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .build()

    stravaAuthLauncher.launch(customTabs.intent.setData(authUri))
}