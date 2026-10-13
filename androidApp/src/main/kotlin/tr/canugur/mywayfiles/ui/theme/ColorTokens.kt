package tr.canugur.mywayfiles.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Enterprise design tokens for the application color system.
 * Semantic naming decoupled from any third-party framework or platform buzzwords.
 */
object AppColors {
    // Surfaces & Canvas
    val CanvasBackground: Color = Color(0xFFF3F3F3)
    val CardSurface: Color = Color(0xFFFFFFFF)
    val CardSurfaceSubtle: Color = Color(0xFFFAFAFA)
    val CardStroke: Color = Color(0xFFE5E5E5)
    val Divider: Color = Color(0xFFEAEAEA)

    // Typography
    val TextPrimary: Color = Color(0xFF202020)
    val TextSecondary: Color = Color(0xFF5E5E5E)
    val TextMuted: Color = Color(0xFF8A8A8A)

    // Brand & Functional Accents
    val BrandAccent: Color = Color(0xFF0067C0)
    val BrandAccentSubtle: Color = Color(0xFFE5F1FB)

    // Storage Health / Capacity Thresholds
    val StatusWarning: Color = Color(0xFFCA5010)
    val StatusCritical: Color = Color(0xFFD13438)
    val ProgressTrack: Color = Color(0xFFE0E0E0)
}
