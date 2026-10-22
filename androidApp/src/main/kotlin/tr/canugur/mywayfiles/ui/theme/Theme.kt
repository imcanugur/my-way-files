package tr.canugur.mywayfiles.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AppTypography = androidx.compose.material3.Typography(
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
)

private val LightColorScheme = lightColorScheme(
    primary = LightColorTokens.brandAccent,
    onPrimary = Color.White,
    secondary = LightColorTokens.textSecondary,
    onSecondary = Color.White,
    background = LightColorTokens.canvasBackground,
    onBackground = LightColorTokens.textPrimary,
    surface = LightColorTokens.cardSurface,
    onSurface = LightColorTokens.textPrimary,
    surfaceVariant = LightColorTokens.cardSurfaceSubtle,
    onSurfaceVariant = LightColorTokens.textSecondary,
    outline = LightColorTokens.cardStroke,
    outlineVariant = LightColorTokens.divider,
    error = LightColorTokens.statusCritical,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkColorTokens.brandAccent,
    onPrimary = Color(0xFF062E6F),
    secondary = DarkColorTokens.textSecondary,
    onSecondary = Color(0xFF202124),
    background = DarkColorTokens.canvasBackground,
    onBackground = DarkColorTokens.textPrimary,
    surface = DarkColorTokens.cardSurface,
    onSurface = DarkColorTokens.textPrimary,
    surfaceVariant = DarkColorTokens.cardSurfaceSubtle,
    onSurfaceVariant = DarkColorTokens.textSecondary,
    outline = DarkColorTokens.cardStroke,
    outlineVariant = DarkColorTokens.divider,
    error = DarkColorTokens.statusCritical,
    onError = Color(0xFF601410)
)

@Composable
fun MyWayFilesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorTokens = if (darkTheme) DarkColorTokens else LightColorTokens
    val materialScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalAppColors provides colorTokens) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = AppTypography,
            content = content
        )
    }
}
