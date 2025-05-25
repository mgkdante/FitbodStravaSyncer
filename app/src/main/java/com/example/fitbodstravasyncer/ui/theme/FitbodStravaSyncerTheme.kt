package com.example.fitbodstravasyncer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
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

// 1) Define your core colors
private val Purple40     = Color(0xFF6650A4)
private val PurpleGrey40 = Color(0xFF625B71)
private val Pink40       = Color(0xFF7D5260)

private val Purple80     = Color(0xFFD0BCFF)
private val PurpleGrey80 = Color(0xFFCCC2DC)
private val Pink80       = Color(0xFFEFB8C8)

// 2) Tonal palettes for light/dark
private val LightTonalScheme = lightColorScheme(
    primary       = Purple40,
    onPrimary     = Color.White,
    secondary     = PurpleGrey40,
    onSecondary   = Color.White,
    tertiary      = Pink40,
    onTertiary    = Color.White,
    // add more roles if you like: background, surface, error, etc.
)

private val DarkTonalScheme = darkColorScheme(
    primary       = Purple80,
    onPrimary     = Color.Black,
    secondary     = PurpleGrey80,
    onSecondary   = Color.Black,
    tertiary      = Pink80,
    onTertiary    = Color.Black,
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
    // override any others as desired
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
        colorScheme = colorScheme,
        typography   = ExpressiveTypography,
        shapes       = ExpressiveShapes,
        motionScheme = MotionScheme.expressive(),  // playful, spring-based motion
    ) {
        content()
    }
}
