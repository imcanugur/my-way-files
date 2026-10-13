package tr.canugur.mywayfiles

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import tr.canugur.mywayfiles.ui.home.HomeScreen
import tr.canugur.mywayfiles.ui.theme.MyWayFilesTheme

@Composable
@Preview
fun App() {
    val systemInDark = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(systemInDark) }

    MyWayFilesTheme(darkTheme = isDarkTheme) {
        HomeScreen(
            isDarkTheme = isDarkTheme,
            onToggleTheme = { isDarkTheme = !isDarkTheme }
        )
    }
}
