package tr.canugur.mywayfiles

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import tr.canugur.mywayfiles.domain.model.StorageDrive
import tr.canugur.mywayfiles.ui.explorer.ExplorerScreen
import tr.canugur.mywayfiles.ui.home.HomeScreen
import tr.canugur.mywayfiles.ui.theme.MyWayFilesTheme
import tr.canugur.mywayfiles.ui.trash.RecycleBinScreen

sealed interface NavigationDestination {
    data object Home : NavigationDestination
    data class Explorer(val drive: StorageDrive, val startInSearch: Boolean = false) : NavigationDestination
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
            transitionSpec = {
                if (initialState is NavigationDestination.Home) {
                    // Forward navigation (Home -> Explorer/RecycleBin): Shared axis slide & fade
                    (slideInHorizontally(
                        initialOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
                        animationSpec = tween(220)
                    ) + fadeIn(animationSpec = tween(220)))
                        .togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> (-fullWidth * 0.15f).toInt() },
                                animationSpec = tween(180)
                            ) + fadeOut(animationSpec = tween(180))
                        )
                } else {
                    // Backward navigation (Explorer/RecycleBin -> Home): Reverse slide & fade
                    (slideInHorizontally(
                        initialOffsetX = { fullWidth -> (-fullWidth * 0.15f).toInt() },
                        animationSpec = tween(220)
                    ) + fadeIn(animationSpec = tween(220)))
                        .togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
                                animationSpec = tween(180)
                            ) + fadeOut(animationSpec = tween(180))
                        )
                }
            },
            label = "ScreenTransition"
        ) { destination ->
            when (destination) {
                is NavigationDestination.Home -> {
                    HomeScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onDriveClick = { drive -> currentDestination = NavigationDestination.Explorer(drive) },
                        onSearchDriveClick = { drive ->
                            currentDestination = NavigationDestination.Explorer(drive, startInSearch = true)
                        },
                        onRecycleBinClick = { currentDestination = NavigationDestination.RecycleBin }
                    )
                }

                is NavigationDestination.Explorer -> {
                    ExplorerScreen(
                        rootPath = destination.drive.path,
                        driveName = destination.drive.name,
                        startInSearch = destination.startInSearch,
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
