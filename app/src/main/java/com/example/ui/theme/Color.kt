package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Main Theme Colors matching reference screenshot
val AppBackgroundDark = Color(0xFF070B14)
val AppSurfaceDark = Color(0xFF0D1424)
val AppCardDark = Color(0xFF10192E)
val AppBorderSubtle = Color(0xFF182642)
val AppBorderGlowing = Color(0xFF1D3560)

// Vibrant Pill and Accent Colors
val AccentBlue = Color(0xFF1B6FFF)
val AccentCyan = Color(0xFF00E5FF)
val AccentGreen = Color(0xFF00E676)
val AccentPurple = Color(0xFF7C4DFF)
val AccentRed = Color(0xFFFF1744)
val AccentAmber = Color(0xFFFFB300)
val WarningAmber = AccentAmber

// Gradients for the 3 Home Action Cards
val LiveCameraGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF0C2B64), Color(0xFF124CA6))
)
val ArmControlGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF05382B), Color(0xFF08684E))
)
val SettingsGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF2C0F4F), Color(0xFF4A1A85))
)

// Card Badge Backgrounds
val LiveCameraIconBg = Color(0xFF1B6FFF)
val ArmControlIconBg = Color(0xFF00BFA5)
val SettingsIconBg = Color(0xFF8E24AA)
val ConnectedGreenBg = Color(0xFF0B3322)
val ConnectedGreenText = Color(0xFF26DE81)

// Typography Colors
val TextWhite = Color(0xFFFFFFFF)
val TextLightGrey = Color(0xFFB0C0D8)
val TextMutedGrey = Color(0xFF627494)

// Material 3 mappings
val PrimaryCyan = AccentCyan
val OnPrimaryDark = Color(0xFF00363D)
val PrimaryContainerDark = Color(0xFF004F58)
val OnPrimaryContainerDark = Color(0xFF80F4FF)

val SecondaryBlue = AccentBlue
val OnSecondaryDark = Color(0xFF003548)
val SecondaryContainerDark = Color(0xFF004D67)

val SurfaceDark = AppBackgroundDark
val SurfaceVariantDark = AppSurfaceDark
val BackgroundDark = AppBackgroundDark
