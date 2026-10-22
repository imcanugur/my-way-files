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
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: (FileItem) -> Unit,
    onLongClick: ((FileItem) -> Unit)? = null,
    onToggleSelect: ((FileItem) -> Unit)? = null,
    onCopy: (FileItem) -> Unit,
    onCut: (FileItem) -> Unit,
    onShare: (FileItem) -> Unit,
    onRename: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit,
    onProperties: (FileItem) -> Unit,
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
            .padding(
                horizontal = Dimensions.SpacingSmall,
                vertical = Dimensions.SpacingMedium
            )
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

            Spacer(modifier = Modifier.height(Dimensions.SpacingMicro))

            val metadataText = buildString {
                val formattedDate = DateTimeFormatter.format(item.lastModifiedMs)
                if (item.isDirectory) {
                    val count = item.childCount
                    if (count != null) {
                        append(
                            if (count == 1) {
                                stringResource(R.string.item_count_single)
                            } else {
                                stringResource(R.string.items_count, count)
                            }
                        )
                        if (formattedDate.isNotEmpty()) append(" • ")
                    }
                } else {
                    append(ByteFormatter.format(item.sizeBytes))
                    if (formattedDate.isNotEmpty()) append(" • ")
                }
                if (formattedDate.isNotEmpty()) {
                    append(formattedDate)
                }
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

        // Context Menu (3-dots) when not in Selection Mode
        if (!isSelectionMode) {
            Box {
                var menuExpanded by remember { mutableStateOf(false) }

                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(Dimensions.ActionButtonSize)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = stringResource(R.string.options_menu),
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(Dimensions.IconSizeMedium)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(AppColors.cardSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.action_copy), color = AppColors.textPrimary) },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.ic_copy), null, tint = AppColors.textSecondary, modifier = Modifier.size(Dimensions.IconSizeMedium))
                        },
                        onClick = {
                            menuExpanded = false
                            onCopy(item)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.action_cut), color = AppColors.textPrimary) },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.ic_cut), null, tint = AppColors.textSecondary, modifier = Modifier.size(Dimensions.IconSizeMedium))
                        },
                        onClick = {
                            menuExpanded = false
                            onCut(item)
                        }
                    )
                    if (!item.isDirectory) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.action_share), color = AppColors.textPrimary) },
                            leadingIcon = {
                                Icon(painterResource(R.drawable.ic_share), null, tint = AppColors.textSecondary, modifier = Modifier.size(Dimensions.IconSizeMedium))
                            },
                            onClick = {
                                menuExpanded = false
                                onShare(item)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.action_rename), color = AppColors.textPrimary) },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.ic_edit_rename), null, tint = AppColors.textSecondary, modifier = Modifier.size(Dimensions.IconSizeMedium))
                        },
                        onClick = {
                            menuExpanded = false
                            onRename(item)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.action_delete), color = AppColors.statusCritical) },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.ic_delete), null, tint = AppColors.statusCritical, modifier = Modifier.size(Dimensions.IconSizeMedium))
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete(item)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.action_properties), color = AppColors.textPrimary) },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.ic_info), null, tint = AppColors.textSecondary, modifier = Modifier.size(Dimensions.IconSizeMedium))
                        },
                        onClick = {
                            menuExpanded = false
                            onProperties(item)
                        }
                    )
                }
            }
        }
    }
}
