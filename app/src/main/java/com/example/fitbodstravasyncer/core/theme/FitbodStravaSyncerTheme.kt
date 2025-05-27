package com.example.fitbodstravasyncer.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme

// Strava orange & Fitbod red
private val StravaOrange = Color(0xFFFC4C02)
private val FitbodRed     = Color(0xFFD32F2F)

// Blend utility
private fun blend(a: Color, b: Color, ratio: Float = 0.5f) = Color(
    red   = a.red   * ratio + b.red   * (1 - ratio),
    green = a.green * ratio + b.green * (1 - ratio),
    blue  = a.blue  * ratio + b.blue  * (1 - ratio),
    alpha = 1f
)

// 2) Tonal palettes for light/dark
private val LightTonalScheme = lightColorScheme(
    primary       = blend(StravaOrange, FitbodRed, 0.5f),  // 50/50
    onPrimary     = Color.White,
    secondary     = StravaOrange,
    onSecondary   = Color.White,
    tertiary      = FitbodRed,
    onTertiary    = Color.White,
    background    = Color(0xFFFDFDFD),
    onBackground  = Color.Black,
    surface       = Color.White,
    onSurface     = Color.Black,
    error         = Color(0xFFB00020),
    onError       = Color.White
)

private val DarkTonalScheme = darkColorScheme(
    primary       = blend(StravaOrange, FitbodRed, 0.4f).copy(alpha = 0.9f), // slightly darker
    onPrimary     = Color.Black,
    secondary     = StravaOrange.copy(alpha = 0.9f),
    onSecondary   = Color.Black,
    tertiary      = FitbodRed.copy(alpha = 0.9f),
    onTertiary    = Color.Black,
    background    = Color(0xFF121212),
    onBackground  = Color.White,
    surface       = Color(0xFF1E1E1E),
    onSurface     = Color.White,
    error         = Color(0xFFCF6679),
    onError       = Color.Black
)

// 3) Expressive shapes
private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(24.dp),
    large      = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(48.dp)
)

// 4) Expressive typography (Roboto Flex default scale)
private val ExpressiveTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)

// 5) The Theme function
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FitbodStravaSyncerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx)
            else          dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkTonalScheme
        else      -> LightTonalScheme
    }

    MaterialExpressiveTheme(
        colorScheme  = colorScheme,
        typography   = ExpressiveTypography,
        shapes       = ExpressiveShapes,
        motionScheme = MotionScheme.expressive(),
    ) {
        content()
    }
}
