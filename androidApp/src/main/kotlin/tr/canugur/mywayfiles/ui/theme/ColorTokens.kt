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
    val progressTrack: Color,
    val mediaCanvas: Color,
    val mediaSurface: Color,
    val mediaSurfaceRaised: Color,
    val mediaControlSurface: Color,
    val mediaControlSurfaceStrong: Color,
    val mediaControlBorder: Color,
    val mediaProgressTrack: Color,
    val mediaTextPrimary: Color,
    val mediaTextSecondary: Color,
    val mediaTextMuted: Color,
    val mediaTextDisabled: Color,
    val mediaAccent: Color,
    val mediaAccentContent: Color,
    val mediaError: Color,
    val mediaErrorMuted: Color
)

val LightColorTokens = ColorTokens(
    canvasBackground = Color(0xFFF8F9FA),
    cardSurface = Color(0xFFFFFFFF),
    cardSurfaceSubtle = Color(0xFFF1F3F4),
    cardStroke = Color(0xFFDADCE0),
    divider = Color(0xFFE8EAED),
    textPrimary = Color(0xFF202124),
    textSecondary = Color(0xFF5F6368),
    textMuted = Color(0xFF70757A),
    brandAccent = Color(0xFF0B57D0),
    brandAccentSubtle = Color(0xFFE8F0FE),
    statusWarning = Color(0xFFB06000),
    statusCritical = Color(0xFFB3261E),
    progressTrack = Color(0xFFE8EAED),
    // Media viewing intentionally remains immersive and dark regardless of app theme.
    mediaCanvas = Color(0xFF0A0A0A),
    mediaSurface = Color(0xFF141414),
    mediaSurfaceRaised = Color(0xFF2A2A2A),
    mediaControlSurface = Color(0x99202020),
    mediaControlSurfaceStrong = Color(0xEE181818),
    mediaControlBorder = Color(0x33FFFFFF),
    mediaProgressTrack = Color(0xFF404040),
    mediaTextPrimary = Color.White,
    mediaTextSecondary = Color(0xFFB0B0B0),
    mediaTextMuted = Color(0xFF9E9E9E),
    mediaTextDisabled = Color(0x66FFFFFF),
    mediaAccent = Color(0xFF4CC2FF),
    mediaAccentContent = Color.Black,
    mediaError = Color(0xFFFF6B6B),
    mediaErrorMuted = Color(0xFFFF8A80)
)

val DarkColorTokens = ColorTokens(
    canvasBackground = Color(0xFF131314),
    cardSurface = Color(0xFF1E1F20),
    cardSurfaceSubtle = Color(0xFF28292A),
    cardStroke = Color(0xFF3C4043),
    divider = Color(0xFF303134),
    textPrimary = Color(0xFFE8EAED),
    textSecondary = Color(0xFFBDC1C6),
    textMuted = Color(0xFF9AA0A6),
    brandAccent = Color(0xFFA8C7FA),
    brandAccentSubtle = Color(0xFF26364D),
    statusWarning = Color(0xFFF9AB00),
    statusCritical = Color(0xFFF2B8B5),
    progressTrack = Color(0xFF3C4043),
    // Keep media controls legible over content in both application themes.
    mediaCanvas = Color(0xFF0A0A0A),
    mediaSurface = Color(0xFF141414),
    mediaSurfaceRaised = Color(0xFF2A2A2A),
    mediaControlSurface = Color(0x99202020),
    mediaControlSurfaceStrong = Color(0xEE181818),
    mediaControlBorder = Color(0x33FFFFFF),
    mediaProgressTrack = Color(0xFF404040),
    mediaTextPrimary = Color.White,
    mediaTextSecondary = Color(0xFFB0B0B0),
    mediaTextMuted = Color(0xFF9E9E9E),
    mediaTextDisabled = Color(0x66FFFFFF),
    mediaAccent = Color(0xFF4CC2FF),
    mediaAccentContent = Color.Black,
    mediaError = Color(0xFFFF6B6B),
    mediaErrorMuted = Color(0xFFFF8A80)
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
    val mediaCanvas: Color @Composable get() = LocalAppColors.current.mediaCanvas
    val mediaSurface: Color @Composable get() = LocalAppColors.current.mediaSurface
    val mediaSurfaceRaised: Color @Composable get() = LocalAppColors.current.mediaSurfaceRaised
    val mediaControlSurface: Color @Composable get() = LocalAppColors.current.mediaControlSurface
    val mediaControlSurfaceStrong: Color @Composable get() = LocalAppColors.current.mediaControlSurfaceStrong
    val mediaControlBorder: Color @Composable get() = LocalAppColors.current.mediaControlBorder
    val mediaProgressTrack: Color @Composable get() = LocalAppColors.current.mediaProgressTrack
    val mediaTextPrimary: Color @Composable get() = LocalAppColors.current.mediaTextPrimary
    val mediaTextSecondary: Color @Composable get() = LocalAppColors.current.mediaTextSecondary
    val mediaTextMuted: Color @Composable get() = LocalAppColors.current.mediaTextMuted
    val mediaTextDisabled: Color @Composable get() = LocalAppColors.current.mediaTextDisabled
    val mediaAccent: Color @Composable get() = LocalAppColors.current.mediaAccent
    val mediaAccentContent: Color @Composable get() = LocalAppColors.current.mediaAccentContent
    val mediaError: Color @Composable get() = LocalAppColors.current.mediaError
    val mediaErrorMuted: Color @Composable get() = LocalAppColors.current.mediaErrorMuted
}
