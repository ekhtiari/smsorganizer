package com.ekhtiari.smsorganizer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val GreenPrimary = Color(0xFF1B7A5A)
private val GreenDark = Color(0xFF0B3D2E)
private val GreenContainer = Color(0xFFD4EFE4)
private val AmberAccent = Color(0xFFC47B16)
private val SurfaceLight = Color(0xFFF3F7F5)
private val SurfaceDark = Color(0xFF101814)

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = GreenDark,
    secondary = AmberAccent,
    onSecondary = Color.White,
    background = SurfaceLight,
    onBackground = Color(0xFF12201A),
    surface = Color.White,
    onSurface = Color(0xFF12201A),
    surfaceVariant = Color(0xFFE2EDE8),
    onSurfaceVariant = Color(0xFF3E5249),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6BC9A4),
    onPrimary = GreenDark,
    primaryContainer = Color(0xFF145C44),
    onPrimaryContainer = GreenContainer,
    secondary = Color(0xFFE0A45A),
    onSecondary = Color(0xFF3A2500),
    background = SurfaceDark,
    onBackground = Color(0xFFE4EEE9),
    surface = Color(0xFF16211C),
    onSurface = Color(0xFFE4EEE9),
    surfaceVariant = Color(0xFF2A3832),
    onSurfaceVariant = Color(0xFFB8C9C0),
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
)

@Composable
fun SmsOrganizerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
