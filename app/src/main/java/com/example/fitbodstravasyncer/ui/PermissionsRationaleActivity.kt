package com.example.fitbodstravasyncer.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                UiStrings.WHY_THIS_PERMISSION,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "This app needs access to your Health Connect data " +
                                        "to read workouts, calories, and heart rate.\n\n" +
                                        "We only use this data to sync your Fitbod sessions with Strava.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(32.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = {
                                        setResult(Activity.RESULT_OK)
                                        finish()
                                    }
                                ) {
                                    Text(UiStrings.ALLOW)
                                }
                                OutlinedButton(
                                    onClick = {
                                        setResult(Activity.RESULT_CANCELED)
                                        finish()
                                    }
                                ) {
                                    Text(UiStrings.DENY)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
