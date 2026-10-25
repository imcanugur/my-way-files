package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.core.util.FileTypeClassifier
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.common.AppModalBottomSheet
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions
import java.io.File

/** Shared single-item action sheet for list and grid. Only actions backed by real handlers are offered. */
@Composable
fun FileItemActionsMenu(
    item: FileItem,
    isFavorite: Boolean,
    onCopy: (FileItem) -> Unit,
    onCut: (FileItem) -> Unit,
    onMoveToFolder: (FileItem) -> Unit,
    onShare: (FileItem) -> Unit,
    onRename: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit,
    onProperties: (FileItem) -> Unit,
    onToggleFavorite: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var showActions by remember(item.path) { mutableStateOf(false) }
    IconButton(onClick = { showActions = true }, modifier = modifier.size(Dimensions.ActionButtonSize)) {
        Icon(
            painter = painterResource(R.drawable.ic_more_vert),
            contentDescription = stringResource(R.string.options_menu),
            tint = AppColors.textSecondary,
            modifier = Modifier.size(Dimensions.IconSizeMedium)
        )
    }

    if (showActions) {
        AppModalBottomSheet(onDismissRequest = { showActions = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = Dimensions.SpacingLarge)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = Dimensions.SpacingLarge, vertical = Dimensions.SpacingSmall)
                )
                Text(
                    text = if (item.isDirectory) File(item.path).parent.orEmpty()
                    else "${File(item.path).parent.orEmpty()} · ${ByteFormatter.format(item.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = Dimensions.SpacingLarge).padding(bottom = Dimensions.SpacingSmall)
                )
                HorizontalDivider(color = AppColors.divider)
                ActionRow(
                    if (isFavorite) R.string.remove_favorite else R.string.add_favorite,
                    R.drawable.ic_favorite,
                    if (isFavorite) AppColors.brandAccent else AppColors.textSecondary
                ) { showActions = false; onToggleFavorite(item) }
                ActionRow(R.string.action_copy, R.drawable.ic_copy) { showActions = false; onCopy(item) }
                ActionRow(R.string.action_cut, R.drawable.ic_cut) { showActions = false; onCut(item) }
                ActionRow(R.string.action_move_to_folder, R.drawable.ic_folder) {
                    showActions = false; onMoveToFolder(item)
                }
                if (!item.isDirectory) ActionRow(R.string.action_share, R.drawable.ic_share) {
                    showActions = false; onShare(item)
                }
                ActionRow(R.string.action_rename, R.drawable.ic_edit_rename) { showActions = false; onRename(item) }
                ActionRow(R.string.action_properties, R.drawable.ic_info) { showActions = false; onProperties(item) }
                HorizontalDivider(color = AppColors.divider, modifier = Modifier.padding(top = Dimensions.SpacingExtraSmall))
                ActionRow(R.string.action_delete, R.drawable.ic_delete, AppColors.statusCritical) {
                    showActions = false; onDelete(item)
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    label: Int,
    icon: Int,
    tint: androidx.compose.ui.graphics.Color = AppColors.textSecondary,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(stringResource(label), color = if (tint == AppColors.statusCritical) tint else AppColors.textPrimary)
        },
        leadingContent = {
            Icon(painter = painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(Dimensions.IconSizeMedium))
        },
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Dimensions.SpacingSmall),
        colors = ListItemDefaults.colors(containerColor = AppColors.cardSurface)
    )
}
