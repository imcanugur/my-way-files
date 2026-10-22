package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * Enterprise list item rendering a directory or file entry with precise metadata,
 * multi-selection support, hidden file visual distinction, and contextual actions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    item: FileItem,
    isFavorite: Boolean,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    compact: Boolean = false,
    onClick: (FileItem) -> Unit,
    onLongClick: ((FileItem) -> Unit)? = null,
    onToggleSelect: ((FileItem) -> Unit)? = null,
    onCopy: (FileItem) -> Unit,
    onCut: (FileItem) -> Unit,
    onShare: (FileItem) -> Unit,
    onRename: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit,
    onProperties: (FileItem) -> Unit,
    onToggleFavorite: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(Dimensions.CornerRadiusMedium)
    val rowBackground = if (isSelected) AppColors.brandAccentSubtle else androidx.compose.ui.graphics.Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(rowBackground)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect?.invoke(item)
                    } else {
                        onClick(item)
                    }
                },
                onLongClick = {
                    if (isSelectionMode) {
                        onToggleSelect?.invoke(item)
                    } else {
                        onLongClick?.invoke(item)
                    }
                },
                role = Role.Button
            )
            .semantics {
                if (isSelectionMode) selected = isSelected
            }
            .padding(horizontal = Dimensions.SpacingSmall, vertical = if (compact) Dimensions.SpacingSmall else Dimensions.SpacingMedium)
            .alpha(if (item.isHidden) 0.62f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection Checkbox in Selection Mode
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .size(Dimensions.SpacingLarge)
                    .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                    .background(if (isSelected) AppColors.brandAccent else AppColors.cardSurfaceSubtle)
                    .border(
                        width = Dimensions.CardBorderWidth,
                        color = if (isSelected) AppColors.brandAccent else AppColors.cardStroke,
                        shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = AppColors.canvasBackground,
                        modifier = Modifier.size(Dimensions.IconSizeSmall)
                    )
                }
            }

            Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))
        }

        // Leading Thumbnail or Vector Icon
        FileThumbnailView(item = item)

        Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

        // Name and Metadata
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = AppColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!compact) {
                Spacer(modifier = Modifier.height(Dimensions.SpacingMicro))
                val metadataText = buildString {
                    val formattedDate = DateTimeFormatter.format(item.lastModifiedMs)
                    if (!item.isDirectory) {
                        append(ByteFormatter.format(item.sizeBytes))
                        if (formattedDate.isNotEmpty()) append(" • ")
                    }
                    if (formattedDate.isNotEmpty()) append(formattedDate)
                }
                if (metadataText.isNotEmpty()) {
                    Text(
                        text = metadataText,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (!isSelectionMode) {
            FileItemActionsMenu(
                item = item,
                isFavorite = isFavorite,
                onCopy = onCopy,
                onCut = onCut,
                onShare = onShare,
                onRename = onRename,
                onDelete = onDelete,
                onProperties = onProperties,
                onToggleFavorite = onToggleFavorite
            )
        }
    }
}
