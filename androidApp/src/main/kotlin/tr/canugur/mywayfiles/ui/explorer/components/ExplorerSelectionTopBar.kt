package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/** Compact contextual app bar for Explorer selection mode. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerSelectionTopBar(
    selectedCount: Int,
    isAllSelected: Boolean,
    onCloseSelection: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onBatchCopy: () -> Unit,
    onBatchShare: () -> Unit,
    canShareSelected: Boolean,
    onBatchCut: () -> Unit,
    onBatchMoveToFolder: () -> Unit,
    canMoveSelected: Boolean,
    onBatchDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var actionsExpanded by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compactToolbar = maxWidth < Dimensions.CompactActionBreakpoint
        TopAppBar(
        navigationIcon = {
            IconButton(onClick = onCloseSelection) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.action_cancel),
                    tint = AppColors.textPrimary
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.selected_count, selectedCount),
                style = if (compactToolbar) MaterialTheme.typography.titleMedium
                else MaterialTheme.typography.titleLarge,
                color = AppColors.textPrimary,
                maxLines = 1
            )
        },
        actions = {
            if (!compactToolbar && selectedCount > 0 && canShareSelected) {
                IconButton(onClick = onBatchShare) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = stringResource(R.string.action_share),
                        tint = AppColors.textSecondary
                    )
                }
            }
            if (!compactToolbar && selectedCount > 0) {
                if (canMoveSelected) {
                    IconButton(onClick = onBatchMoveToFolder) {
                        Icon(
                            painter = painterResource(R.drawable.ic_folder),
                            contentDescription = stringResource(R.string.action_move_to_folder),
                            tint = AppColors.textSecondary
                        )
                    }
                }
                IconButton(onClick = onBatchCopy) {
                    Icon(
                        painter = painterResource(R.drawable.ic_copy),
                        contentDescription = stringResource(R.string.action_copy),
                        tint = AppColors.textSecondary
                    )
                }
                IconButton(onClick = onBatchDelete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.action_delete),
                        tint = AppColors.statusCritical
                    )
                }
            }
            Box {
                IconButton(onClick = { actionsExpanded = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = stringResource(R.string.options_menu),
                        tint = AppColors.textPrimary
                    )
                }
                DropdownMenu(
                    expanded = actionsExpanded,
                    onDismissRequest = { actionsExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(if (isAllSelected) R.string.deselect_all else R.string.select_all)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_select_all),
                                contentDescription = null
                            )
                        },
                        onClick = {
                            actionsExpanded = false
                            onToggleSelectAll()
                        }
                    )
                    if (selectedCount > 0) {
                        if (compactToolbar && canShareSelected) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_share)) },
                                leadingIcon = {
                                    Icon(painter = painterResource(R.drawable.ic_share), contentDescription = null)
                                },
                                onClick = {
                                    actionsExpanded = false
                                    onBatchShare()
                                }
                            )
                        }
                        if (compactToolbar) {
                            if (canMoveSelected) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_move_to_folder)) },
                                    leadingIcon = { Icon(painter = painterResource(R.drawable.ic_folder), contentDescription = null) },
                                    onClick = { actionsExpanded = false; onBatchMoveToFolder() }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_copy)) },
                                leadingIcon = {
                                    Icon(painter = painterResource(R.drawable.ic_copy), contentDescription = null)
                                },
                                onClick = {
                                    actionsExpanded = false
                                    onBatchCopy()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete)) },
                                leadingIcon = {
                                    Icon(painter = painterResource(R.drawable.ic_delete), contentDescription = null)
                                },
                                onClick = {
                                    actionsExpanded = false
                                    onBatchDelete()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_cut)) },
                            leadingIcon = {
                                Icon(painter = painterResource(R.drawable.ic_cut), contentDescription = null)
                            },
                            onClick = {
                                actionsExpanded = false
                                onBatchCut()
                            }
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.canvasBackground)
        )
    }
}
