package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/** The shared per-item action menu for list and grid presentations. */
@Composable
fun FileItemActionsMenu(
    item: FileItem,
    isFavorite: Boolean,
    onCopy: (FileItem) -> Unit,
    onCut: (FileItem) -> Unit,
    onShare: (FileItem) -> Unit,
    onRename: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit,
    onProperties: (FileItem) -> Unit,
    onToggleFavorite: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(Dimensions.ActionButtonSize)) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.options_menu),
                tint = AppColors.textSecondary,
                modifier = Modifier.size(Dimensions.IconSizeMedium)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AppColors.cardSurface)
        ) {
            ActionItem(
                label = if (isFavorite) R.string.remove_favorite else R.string.add_favorite,
                icon = R.drawable.ic_favorite,
                tint = if (isFavorite) AppColors.brandAccent else AppColors.textSecondary,
                onClick = { onToggleFavorite(item); expanded = false }
            )
            ActionItem(R.string.action_copy, R.drawable.ic_copy) { onCopy(item); expanded = false }
            ActionItem(R.string.action_cut, R.drawable.ic_cut) { onCut(item); expanded = false }
            if (!item.isDirectory) {
                ActionItem(R.string.action_share, R.drawable.ic_share) { onShare(item); expanded = false }
            }
            ActionItem(R.string.action_rename, R.drawable.ic_edit_rename) { onRename(item); expanded = false }
            ActionItem(
                R.string.action_delete,
                R.drawable.ic_delete,
                tint = AppColors.statusCritical,
                destructive = true
            ) { onDelete(item); expanded = false }
            ActionItem(R.string.action_properties, R.drawable.ic_info) { onProperties(item); expanded = false }
        }
    }
}

@Composable
private fun ActionItem(
    label: Int,
    icon: Int,
    tint: androidx.compose.ui.graphics.Color = AppColors.textSecondary,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(stringResource(label), color = if (destructive) tint else AppColors.textPrimary) },
        leadingIcon = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(Dimensions.IconSizeMedium)
            )
        },
        onClick = onClick
    )
}
