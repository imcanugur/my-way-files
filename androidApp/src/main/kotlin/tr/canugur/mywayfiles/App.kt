package tr.canugur.mywayfiles

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import tr.canugur.mywayfiles.domain.model.StorageDrive
import tr.canugur.mywayfiles.ui.explorer.ExplorerScreen
import tr.canugur.mywayfiles.ui.home.HomeScreen
import tr.canugur.mywayfiles.ui.theme.MyWayFilesTheme

@Composable
@Preview
fun App() {
    val systemInDark = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(systemInDark) }
    var selectedDrive by remember { mutableStateOf<StorageDrive?>(null) }

    MyWayFilesTheme(darkTheme = isDarkTheme) {
        AnimatedContent(
            targetState = selectedDrive,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ScreenTransition"
        ) { drive ->
            if (drive != null) {
                ExplorerScreen(
                    rootPath = drive.path,
                    driveName = drive.name,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme },
                    onNavigateBack = { selectedDrive = null }
                )
            } else {
                HomeScreen(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme },
                    onDriveClick = { selectedDrive = it }
                )
            }
        }
    }
}
