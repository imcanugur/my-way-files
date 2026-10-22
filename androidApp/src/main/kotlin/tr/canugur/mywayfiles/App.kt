package tr.canugur.mywayfiles

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File
import tr.canugur.mywayfiles.data.preferences.SavedLocation
import tr.canugur.mywayfiles.data.preferences.SavedLocationStore
import tr.canugur.mywayfiles.data.preferences.AppSettingsStore
import tr.canugur.mywayfiles.data.preferences.ExplorerPreferences
import tr.canugur.mywayfiles.domain.model.StorageDrive
import tr.canugur.mywayfiles.domain.model.GridTileSize
import tr.canugur.mywayfiles.ui.navigation.AppNavigationDrawer
import tr.canugur.mywayfiles.ui.navigation.AppSettingsScreen
import tr.canugur.mywayfiles.ui.explorer.ExplorerScreen
import tr.canugur.mywayfiles.ui.explorer.FileClipboardStore
import tr.canugur.mywayfiles.ui.home.HomeScreen
import tr.canugur.mywayfiles.ui.theme.MyWayFilesTheme
import tr.canugur.mywayfiles.ui.theme.MotionTokens
import tr.canugur.mywayfiles.ui.trash.RecycleBinScreen

sealed interface NavigationDestination {
    data object Home : NavigationDestination
    data object Settings : NavigationDestination
    data class Explorer(
        val drive: StorageDrive,
        val startInSearch: Boolean = false,
        val initialPath: String = drive.path,
        val initialFilePath: String? = null
    ) : NavigationDestination
    data object RecycleBin : NavigationDestination
}

@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
fun App() {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()
    val appSettingsStore = remember(context) { AppSettingsStore(context) }
    val explorerPreferences = remember(context) { ExplorerPreferences(context) }
    var isDarkTheme by remember(appSettingsStore) {
        mutableStateOf(appSettingsStore.darkThemeEnabled(systemInDark))
    }
    var confirmBeforeMovingToTrash by remember(appSettingsStore) {
        mutableStateOf(appSettingsStore.confirmBeforeMovingToTrash())
    }
    var confirmBeforePermanentDelete by remember(appSettingsStore) {
        mutableStateOf(appSettingsStore.confirmBeforePermanentDelete())
    }
    var showHiddenFiles by remember(explorerPreferences) {
        mutableStateOf(explorerPreferences.showHiddenFiles)
    }
    var explorerViewMode by remember(explorerPreferences) { mutableStateOf(explorerPreferences.viewMode) }
    var gridTileSize by remember(explorerPreferences) { mutableStateOf(explorerPreferences.gridTileSize) }
    var currentDestination by remember { mutableStateOf<NavigationDestination>(NavigationDestination.Home) }
    var destinationBeforeSettings by remember { mutableStateOf<NavigationDestination>(NavigationDestination.Home) }
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val locationStore = remember(context) { SavedLocationStore(context) }
    val fileClipboardStore = remember { FileClipboardStore() }
    var storageDrives by remember { mutableStateOf(emptyList<StorageDrive>()) }
    var recentFolders by remember { mutableStateOf(locationStore.recentFolders()) }
    var recentFolderLimit by remember { mutableStateOf(locationStore.recentFolderLimit) }
    var favorites by remember { mutableStateOf(locationStore.favorites()) }
    val navigationOffsetPx = with(LocalDensity.current) {
        MotionTokens.NAVIGATION_OFFSET_DP.dp.roundToPx()
    }

    fun openExplorer(drive: StorageDrive, path: String = drive.path, filePath: String? = null) {
        currentDestination = NavigationDestination.Explorer(
            drive = drive,
            initialPath = path,
            initialFilePath = filePath
        )
    }

    fun openSettings() {
        if (currentDestination !is NavigationDestination.Settings) {
            destinationBeforeSettings = currentDestination
        }
        currentDestination = NavigationDestination.Settings
    }

    fun driveForPath(path: String): StorageDrive? = storageDrives
        .filter { path == it.path || path.startsWith(it.path.trimEnd(File.separatorChar) + File.separator) }
        .maxByOrNull { it.path.length }

    fun selectLocation(location: SavedLocation) {
        val drive = driveForPath(location.path)
        if (drive != null) {
            val directory = if (location.isDirectory) location.path else File(location.path).parent ?: drive.path
            openExplorer(drive, directory, location.path.takeUnless { location.isDirectory })
        }
    }

    suspend fun closeDrawer() {
        if (drawerState.isOpen) drawerState.close()
    }

    fun openDrawer() {
        recentFolders = locationStore.recentFolders()
        recentFolderLimit = locationStore.recentFolderLimit
        favorites = locationStore.favorites()
        scope.launch { drawerState.open() }
    }

    MyWayFilesTheme(darkTheme = isDarkTheme) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.widthIn(max = tr.canugur.mywayfiles.ui.theme.Dimensions.NavigationDrawerMaxWidth)) {
                    AppNavigationDrawer(
                        drives = storageDrives,
                        recentFolders = recentFolders,
                        recentFolderLimit = recentFolderLimit,
                        favorites = favorites,
                        selectedKey = when (val destination = currentDestination) {
                            is NavigationDestination.Home -> "home"
                            is NavigationDestination.Settings -> "settings"
                            is NavigationDestination.RecycleBin -> "trash"
                            is NavigationDestination.Explorer -> if (destination.initialPath == destination.drive.path) {
                                "drive:${destination.drive.path}"
                            } else "path:${destination.initialPath}"
                        },
                        onHome = {
                            scope.launch {
                                closeDrawer()
                                currentDestination = NavigationDestination.Home
                            }
                        },
                        onSettings = {
                            scope.launch {
                                closeDrawer()
                                openSettings()
                            }
                        },
                        onDrive = { drive ->
                            scope.launch {
                                closeDrawer()
                                openExplorer(drive)
                            }
                        },
                        onRecent = { location ->
                            scope.launch {
                                closeDrawer()
                                selectLocation(location)
                            }
                        },
                        onFavorite = { location ->
                            scope.launch {
                                closeDrawer()
                                selectLocation(location)
                            }
                        },
                        onRecycleBin = {
                            scope.launch {
                                closeDrawer()
                                currentDestination = NavigationDestination.RecycleBin
                            }
                        }
                    )
                }
            }
        ) {
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                val backward = isBackwardNavigation(initialState, targetState)
                val enterOffset = if (backward) -navigationOffsetPx else navigationOffsetPx
                val exitOffset = -enterOffset
                (slideInHorizontally(
                    initialOffsetX = { enterOffset },
                    animationSpec = tween(MotionTokens.NAVIGATION_ENTER_MS, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(MotionTokens.NAVIGATION_ENTER_MS)))
                    .togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { exitOffset },
                            animationSpec = tween(MotionTokens.NAVIGATION_EXIT_MS, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(MotionTokens.NAVIGATION_EXIT_MS))
                    )
            },
            label = "ScreenTransition"
        ) { destination ->
            when (destination) {
                is NavigationDestination.Home -> {
                    HomeScreen(
                        onDriveClick = { drive -> currentDestination = NavigationDestination.Explorer(drive) },
                        onSearchDriveClick = { drive ->
                            currentDestination = NavigationDestination.Explorer(drive, startInSearch = true)
                        },
                        onRecycleBinClick = { currentDestination = NavigationDestination.RecycleBin },
                        onOpenDrawer = ::openDrawer,
                        onDrivesLoaded = { drives -> storageDrives = drives }
                    )
                }

                is NavigationDestination.Explorer -> {
                    ExplorerScreen(
                        rootPath = destination.drive.path,
                        driveName = destination.drive.name,
                        initialPath = destination.initialPath,
                        initialFilePath = destination.initialFilePath,
                        favoritePaths = favorites.mapTo(mutableSetOf()) { it.path },
                        startInSearch = destination.startInSearch,
                        showHiddenFilesSetting = showHiddenFiles,
                        confirmBeforeMovingToTrash = confirmBeforeMovingToTrash,
                        viewMode = explorerViewMode,
                        gridTileSize = gridTileSize.minWidthDp,
                        onNavigateBack = { currentDestination = NavigationDestination.Home },
                        onOpenDrawer = ::openDrawer,
                        onDirectoryVisited = { path, name, storageRoot, storageName ->
                            locationStore.recordVisit(path, name, true, storageRoot, storageName)
                            recentFolders = locationStore.recentFolders()
                        },
                        onToggleFavorite = { location ->
                            locationStore.toggleFavorite(location.path, location.name, location.isDirectory)
                            favorites = locationStore.favorites()
                        },
                        clipboardStore = fileClipboardStore
                    )
                }

                is NavigationDestination.RecycleBin -> {
                    RecycleBinScreen(
                        confirmBeforePermanentDelete = confirmBeforePermanentDelete,
                        onNavigateBack = { currentDestination = NavigationDestination.Home }
                    )
                }

                is NavigationDestination.Settings -> {
                    AppSettingsScreen(
                        isDarkTheme = isDarkTheme,
                        onDarkThemeChange = { enabled ->
                            appSettingsStore.setDarkThemeEnabled(enabled)
                            isDarkTheme = enabled
                        },
                        showHiddenFiles = showHiddenFiles,
                        onShowHiddenFilesChange = { enabled ->
                            explorerPreferences.showHiddenFiles = enabled
                            showHiddenFiles = enabled
                        },
                        viewMode = explorerViewMode,
                        onViewModeChange = { mode ->
                            explorerPreferences.viewMode = mode
                            explorerViewMode = mode
                        },
                        gridTileSize = gridTileSize,
                        onGridTileSizeChange = { size ->
                            explorerPreferences.gridTileSize = size
                            gridTileSize = size
                        },
                        recentFolderLimit = recentFolderLimit,
                        confirmBeforeMovingToTrash = confirmBeforeMovingToTrash,
                        onConfirmBeforeMovingToTrashChange = { enabled ->
                            appSettingsStore.setConfirmBeforeMovingToTrash(enabled)
                            confirmBeforeMovingToTrash = enabled
                        },
                        confirmBeforePermanentDelete = confirmBeforePermanentDelete,
                        onConfirmBeforePermanentDeleteChange = { enabled ->
                            appSettingsStore.setConfirmBeforePermanentDelete(enabled)
                            confirmBeforePermanentDelete = enabled
                        },
                        onRecentFolderLimitChange = { limit ->
                            locationStore.setRecentFolderLimit(limit)
                            recentFolderLimit = locationStore.recentFolderLimit
                            recentFolders = locationStore.recentFolders()
                        },
                        isDrawerOpen = drawerState.isOpen,
                        onNavigateBack = { currentDestination = destinationBeforeSettings }
                    )
                }
            }
        }
        }
    }
}

private fun isBackwardNavigation(
    from: NavigationDestination,
    to: NavigationDestination
): Boolean = when {
    from is NavigationDestination.Settings -> true
    to is NavigationDestination.Home -> from !is NavigationDestination.Home
    from is NavigationDestination.Explorer && to is NavigationDestination.Explorer ->
        from.drive.path == to.drive.path && isPathAncestor(to.initialPath, from.initialPath)
    else -> false
}

private fun isPathAncestor(ancestor: String, descendant: String): Boolean {
    val normalizedAncestor = ancestor.trimEnd(File.separatorChar)
    return descendant != normalizedAncestor &&
        descendant.startsWith(normalizedAncestor + File.separator)
}
