package tr.canugur.mywayfiles.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Enterprise design tokens for the application theme palette.
 */
@Immutable
data class ColorTokens(
    val canvasBackground: Color,
    val cardSurface: Color,
    val cardSurfaceSubtle: Color,
    val cardStroke: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val brandAccent: Color,
    val brandAccentSubtle: Color,
    val statusWarning: Color,
    val statusCritical: Color,
    val progressTrack: Color
)

val LightColorTokens = ColorTokens(
    canvasBackground = Color(0xFFF3F3F3),
    cardSurface = Color(0xFFFFFFFF),
    cardSurfaceSubtle = Color(0xFFFAFAFA),
    cardStroke = Color(0xFFE5E5E5),
    divider = Color(0xFFEAEAEA),
    textPrimary = Color(0xFF202020),
    textSecondary = Color(0xFF5E5E5E),
    textMuted = Color(0xFF8A8A8A),
    brandAccent = Color(0xFF0067C0),
    brandAccentSubtle = Color(0xFFE5F1FB),
    statusWarning = Color(0xFFCA5010),
    statusCritical = Color(0xFFD13438),
    progressTrack = Color(0xFFE0E0E0)
)

val DarkColorTokens = ColorTokens(
    canvasBackground = Color(0xFF202020),
    cardSurface = Color(0xFF2B2B2B),
    cardSurfaceSubtle = Color(0xFF323232),
    cardStroke = Color(0xFF3A3A3A),
    divider = Color(0xFF3A3A3A),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFD1D1D1),
    textMuted = Color(0xFF9E9E9E),
    brandAccent = Color(0xFF4CC2FF),
    brandAccentSubtle = Color(0xFF1C3B5E),
    statusWarning = Color(0xFFFCE100),
    statusCritical = Color(0xFFFF99A4),
    progressTrack = Color(0xFF404040)
)

val LocalAppColors = staticCompositionLocalOf { LightColorTokens }

/**
 * Global accessor for current theme colors in any Composable.
 */
object AppColors {
    val canvasBackground: Color @Composable get() = LocalAppColors.current.canvasBackground
    val cardSurface: Color @Composable get() = LocalAppColors.current.cardSurface
    val cardSurfaceSubtle: Color @Composable get() = LocalAppColors.current.cardSurfaceSubtle
    val cardStroke: Color @Composable get() = LocalAppColors.current.cardStroke
    val divider: Color @Composable get() = LocalAppColors.current.divider
    val textPrimary: Color @Composable get() = LocalAppColors.current.textPrimary
    val textSecondary: Color @Composable get() = LocalAppColors.current.textSecondary
    val textMuted: Color @Composable get() = LocalAppColors.current.textMuted
    val brandAccent: Color @Composable get() = LocalAppColors.current.brandAccent
    val brandAccentSubtle: Color @Composable get() = LocalAppColors.current.brandAccentSubtle
    val statusWarning: Color @Composable get() = LocalAppColors.current.statusWarning
    val statusCritical: Color @Composable get() = LocalAppColors.current.statusCritical
    val progressTrack: Color @Composable get() = LocalAppColors.current.progressTrack
}
