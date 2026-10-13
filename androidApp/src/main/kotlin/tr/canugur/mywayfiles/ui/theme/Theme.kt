package tr.canugur.mywayfiles.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LightColorTokens.brandAccent,
    onPrimary = Color.White,
    background = LightColorTokens.canvasBackground,
    onBackground = LightColorTokens.textPrimary,
    surface = LightColorTokens.cardSurface,
    onSurface = LightColorTokens.textPrimary,
    surfaceVariant = LightColorTokens.cardSurfaceSubtle,
    onSurfaceVariant = LightColorTokens.textSecondary,
    outline = LightColorTokens.cardStroke
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkColorTokens.brandAccent,
    onPrimary = Color.Black,
    background = DarkColorTokens.canvasBackground,
    onBackground = DarkColorTokens.textPrimary,
    surface = DarkColorTokens.cardSurface,
    onSurface = DarkColorTokens.textPrimary,
    surfaceVariant = DarkColorTokens.cardSurfaceSubtle,
    onSurfaceVariant = DarkColorTokens.textSecondary,
    outline = DarkColorTokens.cardStroke
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
            content = content
        )
    }
}
