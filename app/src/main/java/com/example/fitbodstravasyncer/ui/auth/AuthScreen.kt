package com.example.fitbodstravasyncer.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fitbodstravasyncer.ui.UiStrings

/** Simple two-step onboarding UI */
@Composable
fun AuthScreen(
    hasHealthPermissions: Boolean,
    onRequestHealthPermissions: () -> Unit,
    isStravaConnected: Boolean,
    onConnectStrava: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(UiStrings.WELCOME, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))

        SetupStep(
            completed = hasHealthPermissions,
            label = UiStrings.STEP1_LABEL,
            buttonLabel = UiStrings.STEP1_BTN,
            onClick = onRequestHealthPermissions
        )

        Spacer(Modifier.height(24.dp))

        SetupStep(
            completed = isStravaConnected,
            label = UiStrings.STEP2_LABEL,
            buttonLabel = UiStrings.STEP2_BTN,
            onClick = onConnectStrava
        )

        Spacer(Modifier.height(32.dp))
        if (hasHealthPermissions && isStravaConnected) {
            Text(UiStrings.SETUP_COMPLETE)
        }
    }
}


@Composable
fun SetupStep(
    completed: Boolean,
    label: String,
    buttonLabel: String,
    onClick: () -> Unit
) {
    if (!completed) {
        Text(label)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onClick) {
            Text(buttonLabel)
        }
    } else {
        Text("✔️ $buttonLabel")
    }
}
