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
import tr.canugur.mywayfiles.ui.trash.RecycleBinScreen

sealed interface NavigationDestination {
    data object Home : NavigationDestination
    data class Explorer(val drive: StorageDrive) : NavigationDestination
    data object RecycleBin : NavigationDestination
}

@Composable
@Preview
fun App() {
    val systemInDark = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(systemInDark) }
    var currentDestination by remember { mutableStateOf<NavigationDestination>(NavigationDestination.Home) }

    MyWayFilesTheme(darkTheme = isDarkTheme) {
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ScreenTransition"
        ) { destination ->
            when (destination) {
                is NavigationDestination.Home -> {
                    HomeScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onDriveClick = { drive -> currentDestination = NavigationDestination.Explorer(drive) },
                        onRecycleBinClick = { currentDestination = NavigationDestination.RecycleBin }
                    )
                }

                is NavigationDestination.Explorer -> {
                    ExplorerScreen(
                        rootPath = destination.drive.path,
                        driveName = destination.drive.name,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onNavigateBack = { currentDestination = NavigationDestination.Home }
                    )
                }

                is NavigationDestination.RecycleBin -> {
                    RecycleBinScreen(
                        onNavigateBack = { currentDestination = NavigationDestination.Home }
                    )
                }
            }
        }
    }
}
