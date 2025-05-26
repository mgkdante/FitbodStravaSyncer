package com.example.fitbodstravasyncer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.fitbodstravasyncer.ui.MainScreen
import com.example.fitbodstravasyncer.viewmodel.MainViewModel
import com.example.fitbodstravasyncer.ui.theme.FitbodStravaSyncerTheme

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Theme state (persist with rememberSaveable; you can use DataStore if you want true persistence)
            var appThemeMode by rememberSaveable { mutableStateOf(AppThemeMode.SYSTEM) }
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (appThemeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.LIGHT  -> false
                AppThemeMode.DARK   -> true
            }
            FitbodStravaSyncerTheme(darkTheme = darkTheme) {
                MainScreen(
                    viewModel = viewModel,
                    appThemeMode = appThemeMode,
                    onThemeChange = { appThemeMode = it }
                )
            }
        }
    }
}
