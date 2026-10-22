package tr.canugur.mywayfiles.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized design dimensions and spacing tokens.
 * Strictly eliminates arbitrary magic numbers across the UI layer.
 */
object Dimensions {
    // Spacing & Padding
    val SpacingMicro: Dp = 2.dp
    val SpacingExtraSmall: Dp = 4.dp
    val SpacingSmall: Dp = 8.dp
    val SpacingMedium: Dp = 12.dp
    val SpacingDefault: Dp = 16.dp
    val SpacingLarge: Dp = 20.dp
    val SpacingExtraLarge: Dp = 24.dp
    val SpacingHuge: Dp = 32.dp

    // Corner Radii
    val CornerRadiusNone: Dp = 0.dp
    val CornerRadiusSmall: Dp = 8.dp
    val CornerRadiusDefault: Dp = 12.dp
    val CornerRadiusMedium: Dp = 16.dp
    val CornerRadiusLarge: Dp = 24.dp
    val CornerRadiusPill: Dp = 100.dp

    // Component Dimensions
    val CardBorderWidth: Dp = 1.dp
    val IconSizeSmall: Dp = 16.dp
    val IconSizeMedium: Dp = 20.dp
    val IconSizeDefault: Dp = 24.dp
    val IconSizeLarge: Dp = 36.dp
    val IconContainerSize: Dp = 44.dp

    // Progress Bar
    val ProgressBarHeight: Dp = 6.dp
    val ProgressBarRadius: Dp = 3.dp

    // Header & Actions
    /** Minimum touch target for compact icon actions across app headers and rows. */
    val ActionButtonSize: Dp = 48.dp

    // Responsive layout constraint (available content width after insets and padding).
    val CompactActionBreakpoint: Dp = 380.dp
}
