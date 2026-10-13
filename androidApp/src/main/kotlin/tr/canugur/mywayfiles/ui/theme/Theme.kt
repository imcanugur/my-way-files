package tr.canugur.mywayfiles.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = lightColorScheme(
    primary = AppColors.BrandAccent,
    onPrimary = Color.White,
    background = AppColors.CanvasBackground,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.CardSurface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.CardSurfaceSubtle,
    onSurfaceVariant = AppColors.TextSecondary,
    outline = AppColors.CardStroke
)

@Composable
fun MyWayFilesTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}
