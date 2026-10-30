package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FuturisticDarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0F326E),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = AccentCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004D5A),
    onSecondaryContainer = Color(0xFF8CEEFF),
    tertiary = AccentGreen,
    error = AccentRed,
    background = AppBackgroundDark,
    onBackground = TextWhite,
    surface = AppSurfaceDark,
    onSurface = TextWhite,
    surfaceVariant = AppCardDark,
    onSurfaceVariant = TextLightGrey,
    outline = AppBorderSubtle
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FuturisticDarkColorScheme,
        typography = Typography,
        content = content
    )
}
