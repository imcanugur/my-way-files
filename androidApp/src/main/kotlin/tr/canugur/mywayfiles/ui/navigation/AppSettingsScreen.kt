package tr.canugur.mywayfiles.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.data.preferences.SavedLocationStore
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppSettingsScreen(
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    showHiddenFiles: Boolean,
    onShowHiddenFilesChange: (Boolean) -> Unit,
    recentFolderLimit: Int,
    onRecentFolderLimitChange: (Int) -> Unit,
    confirmBeforeMovingToTrash: Boolean,
    onConfirmBeforeMovingToTrashChange: (Boolean) -> Unit,
    confirmBeforePermanentDelete: Boolean,
    onConfirmBeforePermanentDeleteChange: (Boolean) -> Unit,
    isDrawerOpen: Boolean,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var recentLimitMenuExpanded by remember { mutableStateOf(false) }
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
                    headlineContent = { Text(stringResource(R.string.settings_recent_folders)) },
                    supportingContent = { Text(stringResource(R.string.settings_recent_folders_description)) },
                    trailingContent = {
                        Box {
                            TextButton(onClick = { recentLimitMenuExpanded = true }) {
                                Text(stringResource(R.string.drawer_recent_limit_label, recentFolderLimit))
                            }
                            DropdownMenu(
                                expanded = recentLimitMenuExpanded,
                                onDismissRequest = { recentLimitMenuExpanded = false }
                            ) {
                                SavedLocationStore.RECENT_FOLDER_LIMITS.forEach { limit ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(stringResource(R.string.settings_recent_folders_option, limit))
                                        },
                                        onClick = {
                                            recentLimitMenuExpanded = false
                                            onRecentFolderLimitChange(limit)
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
            item { SettingsSectionTitle(stringResource(R.string.settings_file_operations)) }
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
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_confirm_permanent_delete)) },
                    supportingContent = { Text(stringResource(R.string.settings_confirm_permanent_delete_description)) },
                    trailingContent = {
                        Switch(
                            checked = confirmBeforePermanentDelete,
                            onCheckedChange = onConfirmBeforePermanentDeleteChange
                        )
                    }
                )
            }
        }
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
