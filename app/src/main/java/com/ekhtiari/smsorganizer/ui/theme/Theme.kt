package com.ekhtiari.smsorganizer.ui.theme

import android.os.Build
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ekhtiari.smsorganizer.R

/**
 * Vazirmatn (OFL) covers Persian and Latin, so one family keeps both scripts
 * on the expressive type scale. Letter-spacing stays at 0 so Persian cursive
 * joining is preserved.
 */
private val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
)

private fun type(weight: FontWeight, size: Int, line: Int) = TextStyle(
    fontFamily = Vazirmatn,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = 0.sp,
)

// Warm, saturated fallback for devices without dynamic color. Dynamic color
// still wins on Android 12+.
private val LightColors = lightColorScheme(
    primary = Color(0xFFB32600),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBD1),
    onPrimaryContainer = Color(0xFF3B0900),
    inversePrimary = Color(0xFFFFB5A0),
    secondary = Color(0xFF6B4EA2),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE9DDFF),
    onSecondaryContainer = Color(0xFF23005C),
    tertiary = Color(0xFF006A60),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF9EF2E4),
    onTertiaryContainer = Color(0xFF00201C),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF261814),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF261814),
    surfaceVariant = Color(0xFFF5DDD6),
    onSurfaceVariant = Color(0xFF56423C),
    surfaceTint = Color(0xFFB32600),
    inverseSurface = Color(0xFF3D2C28),
    inverseOnSurface = Color(0xFFFFEDE8),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF89716B),
    outlineVariant = Color(0xFFDCC0B8),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFF8F6),
    surfaceDim = Color(0xFFEED5CE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1EC),
    surfaceContainer = Color(0xFFFFE9E3),
    surfaceContainerHigh = Color(0xFFFBE2DB),
    surfaceContainerHighest = Color(0xFFF5DCD4),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB5A0),
    onPrimary = Color(0xFF5F1500),
    primaryContainer = Color(0xFF8C1E00),
    onPrimaryContainer = Color(0xFFFFDBD1),
    inversePrimary = Color(0xFFB32600),
    secondary = Color(0xFFD0BCFF),
    onSecondary = Color(0xFF3A1D6E),
    secondaryContainer = Color(0xFF523688),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = Color(0xFF82D5C8),
    onTertiary = Color(0xFF003731),
    tertiaryContainer = Color(0xFF005048),
    onTertiaryContainer = Color(0xFF9EF2E4),
    background = Color(0xFF1A100E),
    onBackground = Color(0xFFF8DDD6),
    surface = Color(0xFF1A100E),
    onSurface = Color(0xFFF8DDD6),
    surfaceVariant = Color(0xFF56423C),
    onSurfaceVariant = Color(0xFFDCC0B8),
    surfaceTint = Color(0xFFFFB5A0),
    inverseSurface = Color(0xFFF8DDD6),
    inverseOnSurface = Color(0xFF3D2C28),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFFA48B84),
    outlineVariant = Color(0xFF56423C),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF433531),
    surfaceDim = Color(0xFF1A100E),
    surfaceContainerLowest = Color(0xFF140B09),
    surfaceContainerLow = Color(0xFF231815),
    surfaceContainer = Color(0xFF2A1C19),
    surfaceContainerHigh = Color(0xFF352621),
    surfaceContainerHighest = Color(0xFF41312B),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val AppTypography = Typography(
    displayLarge = type(FontWeight.Bold, 57, 64),
    displayMedium = type(FontWeight.Bold, 45, 52),
    displaySmall = type(FontWeight.Bold, 36, 44),
    headlineLarge = type(FontWeight.Bold, 36, 44),
    headlineMedium = type(FontWeight.Bold, 30, 38),
    headlineSmall = type(FontWeight.SemiBold, 26, 34),
    titleLarge = type(FontWeight.SemiBold, 22, 28),
    titleMedium = type(FontWeight.Medium, 16, 24),
    titleSmall = type(FontWeight.Medium, 14, 20),
    bodyLarge = type(FontWeight.Normal, 16, 24),
    bodyMedium = type(FontWeight.Normal, 14, 20),
    bodySmall = type(FontWeight.Normal, 12, 16),
    labelLarge = type(FontWeight.Medium, 14, 20),
    labelMedium = type(FontWeight.Medium, 12, 16),
    labelSmall = type(FontWeight.Medium, 11, 16),
    displayLargeEmphasized = type(FontWeight.ExtraBold, 57, 64),
    displayMediumEmphasized = type(FontWeight.ExtraBold, 45, 52),
    displaySmallEmphasized = type(FontWeight.ExtraBold, 36, 44),
    headlineLargeEmphasized = type(FontWeight.ExtraBold, 36, 44),
    headlineMediumEmphasized = type(FontWeight.ExtraBold, 30, 38),
    headlineSmallEmphasized = type(FontWeight.ExtraBold, 26, 34),
    titleLargeEmphasized = type(FontWeight.ExtraBold, 22, 28),
    titleMediumEmphasized = type(FontWeight.Bold, 16, 24),
    titleSmallEmphasized = type(FontWeight.Bold, 14, 20),
    bodyLargeEmphasized = type(FontWeight.Medium, 16, 24),
    bodyMediumEmphasized = type(FontWeight.Medium, 14, 20),
    bodySmallEmphasized = type(FontWeight.Medium, 12, 16),
    labelLargeEmphasized = type(FontWeight.Bold, 14, 20),
    labelMediumEmphasized = type(FontWeight.SemiBold, 12, 16),
    labelSmallEmphasized = type(FontWeight.SemiBold, 11, 16),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
    largeIncreased = RoundedCornerShape(24.dp),
    extraLargeIncreased = RoundedCornerShape(36.dp),
    extraExtraLarge = RoundedCornerShape(48.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SmsOrganizerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val reduced = rememberReducedMotion()
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    CompositionLocalProvider(LocalReducedMotion provides reduced) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = if (reduced) ReducedMotionScheme else MotionScheme.expressive(),
            shapes = AppShapes,
            typography = AppTypography,
        ) {
            SharedTransitionLayout {
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides this,
                    content = content,
                )
            }
        }
    }
}
