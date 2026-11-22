package com.ekhtiari.smsorganizer.ui.theme

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
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ekhtiari.smsorganizer.R

/**
 * UI chrome uses the system sans (Roboto). Vazirmatn stays on message text so
 * Persian SMS still joins; Latin UI never depends on it.
 */
private val AppFont = FontFamily.SansSerif

val MessageFontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
)

private fun type(weight: FontWeight, size: Int, line: Int) = TextStyle(
    fontFamily = AppFont,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = 0.sp,
)

/** Message bodies, snippets, drafts, and sender names. Letter-spacing stays 0 so Persian cursive joins. */
fun TextStyle.withMessageFont(): TextStyle = copy(
    fontFamily = MessageFontFamily,
    letterSpacing = 0.sp,
)

// Blue family. Dynamic color is not used — wallpaper colors would replace this on Android 12+.
private val LightColors = lightColorScheme(
    primary = Color(0xFF1A56DB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF001849),
    inversePrimary = Color(0xFFB4C5FF),
    secondary = Color(0xFF3F4E9A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDEE0FF),
    onSecondaryContainer = Color(0xFF00105C),
    tertiary = Color(0xFF00687A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFA6EEFF),
    onTertiaryContainer = Color(0xFF001F26),
    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF121C2B),
    surface = Color(0xFFF7F9FF),
    onSurface = Color(0xFF121C2B),
    surfaceVariant = Color(0xFFDCE3F1),
    onSurfaceVariant = Color(0xFF424754),
    surfaceTint = Color(0xFF1A56DB),
    inverseSurface = Color(0xFF273141),
    inverseOnSurface = Color(0xFFEEF1FA),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF727785),
    outlineVariant = Color(0xFFC2C6D6),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF7F9FF),
    surfaceDim = Color(0xFFD5DCE8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F3FC),
    surfaceContainer = Color(0xFFE8EDF7),
    surfaceContainerHigh = Color(0xFFE2E8F3),
    surfaceContainerHighest = Color(0xFFDCE3EE),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4C5FF),
    onPrimary = Color(0xFF002C70),
    primaryContainer = Color(0xFF00409A),
    onPrimaryContainer = Color(0xFFD8E2FF),
    inversePrimary = Color(0xFF1A56DB),
    secondary = Color(0xFFB9C3FF),
    onSecondary = Color(0xFF1C2670),
    secondaryContainer = Color(0xFF353F86),
    onSecondaryContainer = Color(0xFFDEE0FF),
    tertiary = Color(0xFF84D3E6),
    onTertiary = Color(0xFF003640),
    tertiaryContainer = Color(0xFF004E5C),
    onTertiaryContainer = Color(0xFFA6EEFF),
    background = Color(0xFF0E1520),
    onBackground = Color(0xFFE0E7F4),
    surface = Color(0xFF0E1520),
    onSurface = Color(0xFFE0E7F4),
    surfaceVariant = Color(0xFF424754),
    onSurfaceVariant = Color(0xFFC2C6D6),
    surfaceTint = Color(0xFFB4C5FF),
    inverseSurface = Color(0xFFE0E7F4),
    inverseOnSurface = Color(0xFF273141),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8C909F),
    outlineVariant = Color(0xFF424754),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF343B48),
    surfaceDim = Color(0xFF0E1520),
    surfaceContainerLowest = Color(0xFF090F18),
    surfaceContainerLow = Color(0xFF161D2A),
    surfaceContainer = Color(0xFF1A2230),
    surfaceContainerHigh = Color(0xFF252C3B),
    surfaceContainerHighest = Color(0xFF303746),
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
    content: @Composable () -> Unit,
) {
    val reduced = rememberReducedMotion()
    val colorScheme = if (darkTheme) DarkColors else LightColors

    CompositionLocalProvider(
        LocalReducedMotion provides reduced,
        LocalLayoutDirection provides LayoutDirection.Ltr,
    ) {
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
