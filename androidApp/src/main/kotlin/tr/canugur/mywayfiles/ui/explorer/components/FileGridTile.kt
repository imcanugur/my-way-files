package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/** Large-thumbnail grid cell. Its overflow actions are shared with the list row. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridTile(
    item: FileItem,
    isFavorite: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: (FileItem) -> Unit,
    onLongClick: (FileItem) -> Unit,
    onToggleSelect: (FileItem) -> Unit,
    onCopy: (FileItem) -> Unit,
    onCut: (FileItem) -> Unit,
    onShare: (FileItem) -> Unit,
    onRename: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit,
    onProperties: (FileItem) -> Unit,
    onToggleFavorite: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val tileShape = RoundedCornerShape(Dimensions.CornerRadiusMedium)
    Column(
        modifier = modifier
            .clip(tileShape)
            .background(if (isSelected) AppColors.brandAccentSubtle else androidx.compose.ui.graphics.Color.Transparent)
            .combinedClickable(
                role = Role.Button,
                onClick = { if (isSelectionMode) onToggleSelect(item) else onClick(item) },
                onLongClick = { if (isSelectionMode) onToggleSelect(item) else onLongClick(item) }
            )
            .semantics { if (isSelectionMode) selected = isSelected }
            .padding(Dimensions.SpacingSmall),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                .background(AppColors.cardSurfaceSubtle)
        ) {
            FileThumbnailView(
                item = item,
                modifier = Modifier.fillMaxSize(),
                thumbnailSize = 112.dp
            )
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect(item) },
                    modifier = Modifier.align(Alignment.TopStart)
                )
            } else {
                FileItemActionsMenu(
                    item = item,
                    isFavorite = isFavorite,
                    onCopy = onCopy,
                    onCut = onCut,
                    onShare = onShare,
                    onRename = onRename,
                    onDelete = onDelete,
                    onProperties = onProperties,
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = AppColors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            minLines = 2
        )
        if (!item.isDirectory) {
            Text(
                text = ByteFormatter.format(item.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = stringResource(R.string.folder_label),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textMuted,
                maxLines = 1
            )
        }
    }
}
