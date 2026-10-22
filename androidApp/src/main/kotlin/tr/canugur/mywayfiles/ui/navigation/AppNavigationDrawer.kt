package tr.canugur.mywayfiles.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.data.preferences.SavedLocation
import tr.canugur.mywayfiles.domain.model.StorageDrive
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/** Compact, scrollable destinations for locations the app can actually open. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppNavigationDrawer(
    drives: List<StorageDrive>,
    recentFolders: List<SavedLocation>,
    recentFolderLimit: Int,
    favorites: List<SavedLocation>,
    selectedKey: String,
    onHome: () -> Unit,
    onSettings: () -> Unit,
    onDrive: (StorageDrive) -> Unit,
    onRecent: (SavedLocation) -> Unit,
    onFavorite: (SavedLocation) -> Unit,
    onRecycleBin: () -> Unit
) {
    val favoriteFolders = favorites.filter { it.isDirectory }
    val favoriteFiles = favorites.filterNot { it.isDirectory }
    Column(modifier = Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SpacingLarge, vertical = Dimensions.SpacingHuge),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingExtraSmall)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.textPrimary
            )
            Text(
                text = stringResource(R.string.drawer_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary
            )
        }
        HorizontalDivider(color = AppColors.divider)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = Dimensions.SpacingSmall),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMicro)
        ) {
            item {
                DrawerSectionTitle(stringResource(R.string.drawer_main_section))
                DrawerItem(
                    label = stringResource(R.string.home),
                    icon = R.drawable.ic_home,
                    selected = selectedKey == "home",
                    onClick = onHome
                )
                DrawerItem(
                    label = stringResource(R.string.settings_title),
                    icon = R.drawable.ic_settings,
                    selected = selectedKey == "settings",
                    onClick = onSettings
                )
            }

            if (drives.isNotEmpty()) {
                item { DrawerSectionTitle(stringResource(R.string.drawer_storage_section)) }
                items(drives, key = { "drive:${it.path}" }) { drive ->
                    Column {
                        DrawerItem(
                            label = drive.name,
                            icon = if (drive.isRemovable) R.drawable.ic_drive_sdcard else R.drawable.ic_drive_disk,
                            selected = selectedKey == "drive:${drive.path}",
                            onClick = { onDrive(drive) }
                        )
                        recentFolders.asSequence()
                            .filter { it.storageRoot == drive.path }
                            .take(recentFolderLimit)
                            .forEach { location ->
                                DrawerItem(
                                    label = location.name,
                                    icon = R.drawable.ic_folder,
                                    selected = selectedKey == "path:${location.path}",
                                    supportingText = stringResource(
                                        R.string.drawer_last_location,
                                        location.path
                                    ),
                                    onClick = { onRecent(location) },
                                    modifier = Modifier.padding(start = Dimensions.SpacingLarge)
                                )
                            }
                    }
                }
            }

            if (favoriteFolders.isNotEmpty() || favoriteFiles.isNotEmpty()) {
                item { DrawerSectionTitle(stringResource(R.string.drawer_favorites_section)) }
            }
            if (favoriteFolders.isNotEmpty()) {
                item { DrawerSectionTitle(stringResource(R.string.drawer_favorite_folders)) }
                items(favoriteFolders, key = { "favorite-folder:${it.path}" }) { location ->
                    DrawerItem(
                        label = location.name,
                        icon = R.drawable.ic_folder,
                        selected = selectedKey == "path:${location.path}",
                        onClick = { onFavorite(location) }
                    )
                }
            }
            if (favoriteFiles.isNotEmpty()) {
                item { DrawerSectionTitle(stringResource(R.string.drawer_favorite_files)) }
                items(favoriteFiles, key = { "favorite-file:${it.path}" }) { location ->
                    DrawerItem(
                        label = location.name,
                        icon = R.drawable.ic_file_generic,
                        selected = selectedKey == "path:${location.path}",
                        supportingText = location.path.substringBeforeLast('/', ""),
                        onClick = { onFavorite(location) }
                    )
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Dimensions.SpacingLarge, vertical = Dimensions.SpacingSmall),
                    color = AppColors.divider
                )
                DrawerItem(
                    label = stringResource(R.string.recycle_bin),
                    icon = R.drawable.ic_recycle_bin,
                    selected = selectedKey == "trash",
                    onClick = onRecycleBin
                )
            }
        }
    }
}

@Composable
private fun DrawerSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = AppColors.textSecondary,
        modifier = modifier.padding(
            start = Dimensions.SpacingLarge,
            top = Dimensions.SpacingMedium,
            bottom = Dimensions.SpacingExtraSmall
        )
    )
}

@Composable
private fun DrawerItem(
    label: String,
    icon: Int,
    selected: Boolean,
    onClick: () -> Unit,
    supportingText: String? = null,
    modifier: Modifier = Modifier
) {
    NavigationDrawerItem(
        label = {
            Column {
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                supportingText?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        icon = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(Dimensions.IconSizeMedium)
            )
        },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = AppColors.brandAccentSubtle,
            selectedIconColor = AppColors.brandAccent,
            selectedTextColor = AppColors.textPrimary,
            unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            unselectedIconColor = AppColors.textSecondary,
            unselectedTextColor = AppColors.textPrimary
        ),
        modifier = modifier.padding(horizontal = Dimensions.SpacingSmall)
    )
}
