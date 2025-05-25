package com.example.fitbodstravasyncer.feature.schedule.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

class PermissionsRationaleActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = { TopAppBar(title = { Text("Why We Need Permissions") }) }
                ) { contentPadding ->
                    Column(
                        modifier = Modifier.Companion
                            // 1) consume scaffold insets
                            .padding(contentPadding)
                            // 2) add your own inner margin
                            .padding(16.dp)
                    ) {
                        Text("We use Health Connect to fetch your Fitbod workouts and sync them to Strava.")
                        Spacer(Modifier.Companion.height(8.dp))
                        Button(onClick = {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://yourapp.com/privacy".toUri()
                                )
                            )
                        }) {
                            Text("View Full Privacy Policy")
                        }
                    }
                }
            }
        }
    }
}