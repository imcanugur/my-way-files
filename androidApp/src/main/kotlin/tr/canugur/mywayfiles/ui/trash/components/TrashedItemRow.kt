package tr.canugur.mywayfiles.ui.trash.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.core.util.DateTimeFormatter
import tr.canugur.mywayfiles.domain.model.TrashedItem
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * List item row for a deleted file or folder in the Recycle Bin.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrashedItemRow(
    item: TrashedItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelection: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(Dimensions.CornerRadiusDefault)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) AppColors.brandAccentSubtle else AppColors.canvasBackground)
            .combinedClickable(
                role = Role.Button,
                onClick = { if (isSelectionMode) onToggleSelection() else onClick() },
                onLongClick = onLongClick
            )
            .semantics { if (isSelectionMode) selected = isSelected }
            .padding(horizontal = Dimensions.SpacingSmall, vertical = Dimensions.SpacingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            androidx.compose.material3.Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                modifier = Modifier.size(Dimensions.ActionButtonSize)
            )
            Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
        }

        // High-performance cached thumbnail or folder/file icon
        TrashedItemThumbnailView(item = item)

        Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = AppColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(Dimensions.SpacingMicro))

            item.storageName?.let { storageName ->
                Text(
                    text = stringResource(R.string.trash_storage, storageName),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${stringResource(R.string.original_location)}: ${item.originalPath}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${ByteFormatter.format(item.sizeBytes)} • ${DateTimeFormatter.format(item.deletedAtMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))

        // Actions: Restore & Permanent Delete
        if (!isSelectionMode) Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMicro)) {
            IconButton(
                onClick = onRestore,
                modifier = Modifier.size(Dimensions.ActionButtonSize)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_restore),
                    contentDescription = stringResource(R.string.action_restore),
                    tint = AppColors.brandAccent,
                    modifier = Modifier.size(Dimensions.IconSizeMedium)
                )
            }

            IconButton(
                onClick = onDeletePermanently,
                modifier = Modifier.size(Dimensions.ActionButtonSize)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.action_delete_permanently),
                    tint = AppColors.statusCritical,
                    modifier = Modifier.size(Dimensions.IconSizeMedium)
                )
            }
        }
    }
}
