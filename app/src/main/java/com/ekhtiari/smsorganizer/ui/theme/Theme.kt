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

private val SkyPrimary = Color(0xFF3D9BE8)
private val SkyDeep = Color(0xFF0A3A5C)
private val SkyContainer = Color(0xFFD6EBFA)
private val SlateSecondary = Color(0xFF5B7C99)
private val SurfaceLight = Color(0xFFF4F8FC)
private val SurfaceDark = Color(0xFF0C141C)

private val LightColors = lightColorScheme(
    primary = SkyPrimary,
    onPrimary = Color.White,
    primaryContainer = SkyContainer,
    onPrimaryContainer = SkyDeep,
    secondary = SlateSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E6F0),
    onSecondaryContainer = Color(0xFF1A2F40),
    background = SurfaceLight,
    onBackground = Color(0xFF12202A),
    surface = Color.White,
    onSurface = Color(0xFF12202A),
    surfaceVariant = Color(0xFFE3EEF6),
    onSurfaceVariant = Color(0xFF4A5F70),
    outline = Color(0xFF8AA0B2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BCAEF),
    onPrimary = SkyDeep,
    primaryContainer = Color(0xFF1A4A6E),
    onPrimaryContainer = SkyContainer,
    secondary = Color(0xFF9BB4C9),
    onSecondary = Color(0xFF0E1F2C),
    secondaryContainer = Color(0xFF2A3F50),
    onSecondaryContainer = Color(0xFFD9E6F0),
    background = SurfaceDark,
    onBackground = Color(0xFFE4EEF5),
    surface = Color(0xFF121C26),
    onSurface = Color(0xFFE4EEF5),
    surfaceVariant = Color(0xFF1E2D3A),
    onSurfaceVariant = Color(0xFFA8BCCB),
    outline = Color(0xFF6A8194),
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
