package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.SortOption
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * Universal Explorer overflow dropdown menu containing folder creation,
 * selection mode, hidden files toggle, sorting, and refresh.
 */
@Composable
fun ExplorerOptionsMenu(
    expanded: Boolean,
    showHiddenFiles: Boolean,
    currentSortOption: SortOption,
    onDismissRequest: () -> Unit,
    onCreateFolderClick: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onToggleHiddenFiles: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortSubmenu by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            showSortSubmenu = false
            onDismissRequest()
        },
        modifier = modifier.background(AppColors.cardSurface)
    ) {
        if (!showSortSubmenu) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_new_folder), color = AppColors.textPrimary) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_new_folder),
                        contentDescription = null,
                        tint = AppColors.brandAccent,
                        modifier = Modifier.size(Dimensions.IconSizeMedium)
                    )
                },
                onClick = {
                    onDismissRequest()
                    onCreateFolderClick()
                }
            )

            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.selection_mode), color = AppColors.textPrimary) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(Dimensions.IconSizeMedium)
                    )
                },
                onClick = {
                    onDismissRequest()
                    onEnterSelectionMode()
                }
            )

            HorizontalDivider(color = AppColors.cardStroke)

            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(
                            if (showHiddenFiles) R.string.hide_hidden_files else R.string.show_hidden_files
                        ),
                        color = AppColors.textPrimary
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(
                            if (showHiddenFiles) R.drawable.ic_check else R.drawable.ic_info
                        ),
                        contentDescription = null,
                        tint = if (showHiddenFiles) AppColors.brandAccent else AppColors.textSecondary,
                        modifier = Modifier.size(Dimensions.IconSizeMedium)
                    )
                },
                onClick = {
                    onDismissRequest()
                    onToggleHiddenFiles()
                }
            )

            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.sort_by), color = AppColors.textPrimary) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_select_all),
                        contentDescription = null,
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(Dimensions.IconSizeMedium)
                    )
                },
                onClick = {
                    showSortSubmenu = true
                }
            )

            HorizontalDivider(color = AppColors.cardStroke)

            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.refresh_drives), color = AppColors.textPrimary) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = null,
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(Dimensions.IconSizeMedium)
                    )
                },
                onClick = {
                    onDismissRequest()
                    onRefreshClick()
                }
            )
        } else {
            // Sort Submenu
            DropdownMenuItem(
                text = { Text(text = "${stringResource(R.string.sort_name)} (A-Z)", color = AppColors.textPrimary) },
                leadingIcon = {
                    if (currentSortOption == SortOption.NAME_ASC) {
                        Icon(painterResource(R.drawable.ic_check), null, tint = AppColors.brandAccent, modifier = Modifier.size(Dimensions.IconSizeMedium))
                    }
                },
                onClick = {
                    showSortSubmenu = false
                    onDismissRequest()
                    onSortOptionSelected(SortOption.NAME_ASC)
                }
            )

            DropdownMenuItem(
                text = { Text(text = "${stringResource(R.string.sort_name)} (Z-A)", color = AppColors.textPrimary) },
                leadingIcon = {
                    if (currentSortOption == SortOption.NAME_DESC) {
                        Icon(painterResource(R.drawable.ic_check), null, tint = AppColors.brandAccent, modifier = Modifier.size(Dimensions.IconSizeMedium))
                    }
                },
                onClick = {
                    showSortSubmenu = false
                    onDismissRequest()
                    onSortOptionSelected(SortOption.NAME_DESC)
                }
            )

            DropdownMenuItem(
                text = { Text(text = "${stringResource(R.string.sort_date)} (↓)", color = AppColors.textPrimary) },
                leadingIcon = {
                    if (currentSortOption == SortOption.DATE_DESC) {
                        Icon(painterResource(R.drawable.ic_check), null, tint = AppColors.brandAccent, modifier = Modifier.size(Dimensions.IconSizeMedium))
                    }
                },
                onClick = {
                    showSortSubmenu = false
                    onDismissRequest()
                    onSortOptionSelected(SortOption.DATE_DESC)
                }
            )

            DropdownMenuItem(
                text = { Text(text = "${stringResource(R.string.sort_size)} (↓)", color = AppColors.textPrimary) },
                leadingIcon = {
                    if (currentSortOption == SortOption.SIZE_DESC) {
                        Icon(painterResource(R.drawable.ic_check), null, tint = AppColors.brandAccent, modifier = Modifier.size(Dimensions.IconSizeMedium))
                    }
                },
                onClick = {
                    showSortSubmenu = false
                    onDismissRequest()
                    onSortOptionSelected(SortOption.SIZE_DESC)
                }
            )
        }
    }
}
