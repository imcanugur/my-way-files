package tr.canugur.mywayfiles.ui.navigation

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.text.format.Formatter
import java.text.NumberFormat
import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import kotlin.math.roundToInt
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.constants.ProjectLinks
import tr.canugur.mywayfiles.data.cache.DirectoryCache
import tr.canugur.mywayfiles.data.preferences.SavedLocationLimits
import tr.canugur.mywayfiles.data.preferences.AppSettingsStore
import tr.canugur.mywayfiles.domain.model.FileViewMode
import tr.canugur.mywayfiles.domain.model.GridTileSize
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions
import tr.canugur.mywayfiles.ui.viewer.components.VideoPlaybackMode
import tr.canugur.mywayfiles.ui.viewer.components.labelResource

private data class CacheMemoryInfo(
    val deviceRam: String,
    val regularHeapMb: Int,
    val extendedHeapMb: Int,
    val cacheTarget: String,
    val recommendedScrollHistoryLimit: Int
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppSettingsScreen(
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    showHiddenFiles: Boolean,
    onShowHiddenFilesChange: (Boolean) -> Unit,
    viewMode: FileViewMode,
    onViewModeChange: (FileViewMode) -> Unit,
    gridTileSize: GridTileSize,
    onGridTileSizeChange: (GridTileSize) -> Unit,
    recentFolderLimit: Int,
    onRecentFolderLimitChange: (Int) -> Unit,
    autoHideViewerControls: Boolean,
    onAutoHideViewerControlsChange: (Boolean) -> Unit,
    preferLastMoveTarget: Boolean,
    onPreferLastMoveTargetChange: (Boolean) -> Unit,
    recentMoveTargetLimit: Int,
    onRecentMoveTargetLimitChange: (Int) -> Unit,
    confirmBeforeMovingToTrash: Boolean,
    onConfirmBeforeMovingToTrashChange: (Boolean) -> Unit,
    permanentlyDeleteInsteadOfTrashing: Boolean,
    onPermanentlyDeleteInsteadOfTrashingChange: (Boolean) -> Unit,


    isDrawerOpen: Boolean,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appVersion = remember(context) {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }
    var viewModeMenuExpanded by remember { mutableStateOf(false) }
    var gridSizeMenuExpanded by remember { mutableStateOf(false) }
    var playbackModeMenuExpanded by remember { mutableStateOf(false) }
    val appSettingsStore = remember(context) { AppSettingsStore(context) }
    var directoryCachePercent by remember(appSettingsStore) {
        mutableIntStateOf(appSettingsStore.directoryCacheHeapPercent())
    }
    var useExtendedFolderCache by remember(appSettingsStore) {
        mutableStateOf(appSettingsStore.useExtendedFolderCache())
    }
    var directoryScrollHistoryLimit by remember(appSettingsStore) {
        mutableIntStateOf(appSettingsStore.directoryScrollHistoryLimit())
    }
    val memoryInfo = remember(context, directoryCachePercent, useExtendedFolderCache) {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo().also(manager::getMemoryInfo)
        val budgetMiB = DirectoryCache.memoryCacheBudgetBytes(
            context,
            directoryCachePercent,
            useExtendedFolderCache
        ).toDouble() / (1024.0 * 1024.0)
        val preciseSize = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }.format(budgetMiB) + " MiB (${directoryCachePercent}%)"
        CacheMemoryInfo(
            deviceRam = Formatter.formatShortFileSize(context, info.totalMem),
            regularHeapMb = manager.memoryClass,
            extendedHeapMb = manager.largeMemoryClass,
            cacheTarget = preciseSize,
            recommendedScrollHistoryLimit = appSettingsStore.recommendedDirectoryScrollHistoryLimit(useExtendedFolderCache)
        )
    }
    var playbackMode by remember(appSettingsStore) {
        mutableStateOf(
            runCatching { VideoPlaybackMode.valueOf(appSettingsStore.videoPlaybackModeName()) }
                .getOrDefault(VideoPlaybackMode.PLAY_IN_ORDER)
        )
    }
    BackHandler(enabled = !isDrawerOpen, onBack = onNavigateBack)

    Scaffold(
        modifier = modifier,
        containerColor = AppColors.canvasBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                            tint = AppColors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.canvasBackground,
                    titleContentColor = AppColors.textPrimary
                )
            )
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentPadding = PaddingValues(vertical = Dimensions.SpacingSmall),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMicro)
        ) {
            item { SettingsSectionTitle(stringResource(R.string.settings_appearance)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_dark_theme)) },
                    supportingContent = { Text(stringResource(R.string.settings_dark_theme_description)) },
                    trailingContent = {
                        Switch(checked = isDarkTheme, onCheckedChange = onDarkThemeChange)
                    }
                )
            }
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Dimensions.SpacingLarge),
                    color = AppColors.divider
                )
            }
            item { SettingsSectionTitle(stringResource(R.string.settings_browsing)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_default_view)) },
                    supportingContent = { Text(stringResource(R.string.settings_default_view_description)) },
                    trailingContent = {
                        Box {
                            TextButton(onClick = { viewModeMenuExpanded = true }) {
                                Text(stringResource(viewMode.labelResource()))
                            }
                            DropdownMenu(
                                expanded = viewModeMenuExpanded,
                                onDismissRequest = { viewModeMenuExpanded = false }
                            ) {
                                FileViewMode.values().forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(mode.labelResource())) },
                                        onClick = {
                                            viewModeMenuExpanded = false
                                            onViewModeChange(mode)
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_grid_size)) },
                    supportingContent = { Text(stringResource(R.string.settings_grid_size_description)) },
                    trailingContent = {
                        Box {
                            TextButton(onClick = { gridSizeMenuExpanded = true }) {
                                Text(stringResource(gridTileSize.labelResource()))
                            }
                            DropdownMenu(
                                expanded = gridSizeMenuExpanded,
                                onDismissRequest = { gridSizeMenuExpanded = false }
                            ) {
                                GridTileSize.values().forEach { size ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(size.labelResource())) },
                                        onClick = {
                                            gridSizeMenuExpanded = false
                                            onGridTileSizeChange(size)
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
            item {
                IntegerSliderPreference(
                    title = stringResource(R.string.settings_recent_folders),
                    description = stringResource(
                        R.string.settings_recent_folders_description,
                        SavedLocationLimits.MAX_RECENT_FOLDERS_PER_STORAGE
                    ),
                    value = recentFolderLimit,
                    valueRange = 1f..SavedLocationLimits.MAX_RECENT_FOLDERS_PER_STORAGE.toFloat(),
                    steps = SavedLocationLimits.MAX_RECENT_FOLDERS_PER_STORAGE - 2,
                    onValueChange = onRecentFolderLimitChange
                )
            }
            item {
                IntegerSliderPreference(
                    title = stringResource(R.string.settings_recent_move_targets),
                    description = stringResource(
                        R.string.settings_recent_move_targets_description,
                        SavedLocationLimits.MAX_RECENT_MOVE_TARGETS
                    ),
                    value = recentMoveTargetLimit,
                    valueRange = 0f..SavedLocationLimits.MAX_RECENT_MOVE_TARGETS.toFloat(),
                    steps = SavedLocationLimits.MAX_RECENT_MOVE_TARGETS - 1,
                    onValueChange = onRecentMoveTargetLimitChange
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_hidden_files)) },
                    supportingContent = { Text(stringResource(R.string.settings_hidden_files_description)) },
                    trailingContent = {
                        Switch(checked = showHiddenFiles, onCheckedChange = onShowHiddenFilesChange)
                    }
                )
            }
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Dimensions.SpacingLarge),
                    color = AppColors.divider
                )
            }
            item { SettingsSectionTitle(stringResource(R.string.settings_performance)) }
            item {
                Column {
                    IntegerSliderPreference(
                        title = stringResource(R.string.settings_scroll_position_history),
                        description = stringResource(
                            R.string.settings_scroll_position_history_description,
                            memoryInfo.recommendedScrollHistoryLimit
                        ),
                        value = directoryScrollHistoryLimit,
                        valueRange = AppSettingsStore.MIN_DIRECTORY_SCROLL_HISTORY_LIMIT.toFloat()..
                            AppSettingsStore.MAX_DIRECTORY_SCROLL_HISTORY_LIMIT.toFloat(),
                        steps = AppSettingsStore.MAX_DIRECTORY_SCROLL_HISTORY_LIMIT / 16 - 1,
                        onValueChange = { limit ->
                            directoryScrollHistoryLimit = limit
                            appSettingsStore.setDirectoryScrollHistoryLimit(limit)
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.SpacingLarge),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                val recommended = appSettingsStore.recommendedDirectoryScrollHistoryLimit(useExtendedFolderCache)
                                directoryScrollHistoryLimit = recommended
                                appSettingsStore.setDirectoryScrollHistoryLimit(recommended)
                            }
                        ) {
                            Text(stringResource(R.string.settings_use_recommended))
                        }
                    }
                }
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_memory_cache)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.settings_memory_cache_description,
                                memoryInfo.deviceRam,
                                memoryInfo.regularHeapMb,
                                memoryInfo.extendedHeapMb,
                                memoryInfo.cacheTarget
                            )
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_info),
                            contentDescription = null,
                            tint = AppColors.brandAccent
                        )
                    }
                )
            }
            item {
                IntegerSliderPreference(
                    title = stringResource(R.string.settings_cache_heap_share),
                    description = stringResource(R.string.settings_cache_heap_share_description),
                    value = directoryCachePercent,
                    valueRange = AppSettingsStore.MIN_DIRECTORY_CACHE_HEAP_PERCENT.toFloat()..
                        AppSettingsStore.MAX_DIRECTORY_CACHE_HEAP_PERCENT.toFloat(),
                    steps = AppSettingsStore.MAX_DIRECTORY_CACHE_HEAP_PERCENT -
                        AppSettingsStore.MIN_DIRECTORY_CACHE_HEAP_PERCENT - 1,
                    valueSuffix = "%",
                    onValueChange = { percent ->
                        directoryCachePercent = percent
                        appSettingsStore.setDirectoryCacheHeapPercent(percent)
                        DirectoryCache.getInstance(context).updateMemoryCacheSettings(
                            context,
                            percent,
                            useExtendedFolderCache
                        )
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_extended_cache)) },
                    supportingContent = { Text(stringResource(R.string.settings_extended_cache_description)) },
                    trailingContent = {
                        Switch(
                            checked = useExtendedFolderCache,
                            onCheckedChange = { enabled ->
                                useExtendedFolderCache = enabled
                                appSettingsStore.setUseExtendedFolderCache(enabled)
                                DirectoryCache.getInstance(context).updateMemoryCacheSettings(
                                    context,
                                    directoryCachePercent,
                                    enabled
                                )
                            }
                        )
                    }
                )
            }
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Dimensions.SpacingLarge),
                    color = AppColors.divider
                )
            }
            item { SettingsSectionTitle(stringResource(R.string.settings_viewer)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_auto_hide_viewer_controls)) },
                    supportingContent = {
                        Text(stringResource(R.string.settings_auto_hide_viewer_controls_description))
                    },
                    trailingContent = {
                        Switch(
                            checked = autoHideViewerControls,
                            onCheckedChange = onAutoHideViewerControlsChange
                        )
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.playback_mode)) },
                    supportingContent = { Text(stringResource(R.string.settings_video_playback_mode_description)) },
                    trailingContent = {
                        Box {
                            TextButton(onClick = { playbackModeMenuExpanded = true }) {
                                Text(stringResource(playbackMode.labelResource()))
                            }
                            DropdownMenu(
                                expanded = playbackModeMenuExpanded,
                                onDismissRequest = { playbackModeMenuExpanded = false }
                            ) {
                                VideoPlaybackMode.values().forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(mode.labelResource())) },
                                        onClick = {
                                            playbackModeMenuExpanded = false
                                            playbackMode = mode
                                            appSettingsStore.setVideoPlaybackModeName(mode.name)
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Dimensions.SpacingLarge),
                    color = AppColors.divider
                )
            }
            item { SettingsSectionTitle(stringResource(R.string.settings_file_operations)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_prefer_last_move_target)) },
                    supportingContent = {
                        Text(stringResource(R.string.settings_prefer_last_move_target_description))
                    },
                    trailingContent = {
                        Switch(
                            checked = preferLastMoveTarget,
                            onCheckedChange = onPreferLastMoveTargetChange
                        )
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_permanent_delete_mode)) },
                    supportingContent = {
                        Text(stringResource(R.string.settings_permanent_delete_mode_description))
                    },
                    trailingContent = {
                        Switch(
                            checked = permanentlyDeleteInsteadOfTrashing,
                            onCheckedChange = onPermanentlyDeleteInsteadOfTrashingChange
                        )
                    }
                )
            }
            if (!permanentlyDeleteInsteadOfTrashing) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_confirm_trash)) },
                        supportingContent = { Text(stringResource(R.string.settings_confirm_trash_description)) },
                        trailingContent = {
                            Switch(
                                checked = confirmBeforeMovingToTrash,
                                onCheckedChange = onConfirmBeforeMovingToTrashChange
                            )
                        }
                    )
                }
            }
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Dimensions.SpacingLarge),
                    color = AppColors.divider
                )
            }
            item { SettingsSectionTitle(stringResource(R.string.settings_about)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.app_name)) },
                    supportingContent = { Text(stringResource(R.string.about_app_description)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_info),
                            contentDescription = null,
                            tint = AppColors.brandAccent
                        )
                    },
                    trailingContent = {
                        Text(
                            stringResource(R.string.about_version, appVersion),
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColors.textSecondary
                        )
                    }
                )
            }
            item {
                AboutLinkRow(
                    title = stringResource(R.string.about_repository),
                    description = stringResource(R.string.about_repository_description),
                    icon = R.drawable.ic_file_generic,
                    onClick = { openExternalLink(context, ProjectLinks.REPOSITORY) }
                )
            }
            item {
                AboutLinkRow(
                    title = stringResource(R.string.about_sponsor),
                    description = stringResource(R.string.about_sponsor_description),
                    icon = R.drawable.ic_favorite,
                    onClick = { openExternalLink(context, ProjectLinks.SPONSORS) }
                )
            }
            item {
                AboutLinkRow(
                    title = stringResource(R.string.about_report_issue),
                    description = stringResource(R.string.about_report_issue_description),
                    icon = R.drawable.ic_info,
                    onClick = { openExternalLink(context, ProjectLinks.ISSUES) }
                )
            }
            item {
                AboutLinkRow(
                    title = stringResource(R.string.about_readme),
                    description = stringResource(R.string.about_readme_description),
                    icon = R.drawable.ic_file_generic,
                    onClick = { openExternalLink(context, ProjectLinks.README) }
                )
            }
            item {
                AboutLinkRow(
                    title = stringResource(R.string.about_license),
                    description = stringResource(R.string.about_license_description),
                    icon = R.drawable.ic_info,
                    onClick = { openExternalLink(context, ProjectLinks.LICENSE) }
                )
            }
            item {
                AboutLinkRow(
                    title = stringResource(R.string.about_developer),
                    description = stringResource(R.string.about_developer_description),
                    icon = R.drawable.ic_home,
                    onClick = { openExternalLink(context, ProjectLinks.DEVELOPER_PROFILE) }
                )
            }
        }
    }
}

@Composable
private fun AboutLinkRow(
    title: String,
    description: String,
    icon: Int,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        leadingContent = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = AppColors.textSecondary
            )
        },
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick)
    )
}

private fun openExternalLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.about_link_open_failed, Toast.LENGTH_SHORT).show()
    }
}

private fun FileViewMode.labelResource(): Int = when (this) {
    FileViewMode.LIST -> R.string.view_list
    FileViewMode.COMPACT -> R.string.view_compact
    FileViewMode.GRID -> R.string.view_grid
}

private fun GridTileSize.labelResource(): Int = when (this) {
    GridTileSize.SMALL -> R.string.settings_grid_size_small
    GridTileSize.MEDIUM -> R.string.settings_grid_size_medium
    GridTileSize.LARGE -> R.string.settings_grid_size_large
}

@Composable
private fun IntegerSliderPreference(
    title: String,
    description: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueSuffix: String = "",
    onValueChange: (Int) -> Unit
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(
            horizontal = Dimensions.SpacingLarge,
            vertical = Dimensions.SpacingSmall
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = AppColors.textPrimary)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary
                )
            }
            Text(
                text = "${sliderValue.roundToInt()}$valueSuffix",
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.brandAccent,
                modifier = Modifier.padding(start = Dimensions.SpacingMedium)
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {
                val selectedValue = sliderValue.roundToInt()
                sliderValue = selectedValue.toFloat()
                onValueChange(selectedValue)
            },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = title
                stateDescription = "${sliderValue.roundToInt()}$valueSuffix"
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = AppColors.textSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = Dimensions.SpacingLarge,
                vertical = Dimensions.SpacingSmall
            )
    )
}
