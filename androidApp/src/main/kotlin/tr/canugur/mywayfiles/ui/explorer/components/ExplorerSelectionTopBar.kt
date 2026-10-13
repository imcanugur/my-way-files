package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * Enterprise Selection Mode TopBar row displaying selection counts and batch actions.
 */
@Composable
fun ExplorerSelectionTopBar(
    selectedCount: Int,
    isAllSelected: Boolean,
    onCloseSelection: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onBatchCopy: () -> Unit,
    onBatchCut: () -> Unit,
    onBatchDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Close Button + Count Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimensions.ActionButtonSize)
                    .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                    .background(AppColors.cardSurfaceSubtle)
                    .border(
                        width = Dimensions.CardBorderWidth,
                        color = AppColors.cardStroke,
                        shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
                    )
                    .clickable { onCloseSelection() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close_viewer),
                    tint = AppColors.textPrimary,
                    modifier = Modifier.size(Dimensions.IconSizeMedium)
                )
            }

            Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

            Text(
                text = if (selectedCount > 0) {
                    stringResource(R.string.selected_count, selectedCount)
                } else {
                    stringResource(R.string.selection_mode)
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (selectedCount > 0) AppColors.brandAccent else AppColors.textPrimary
            )
        }

        // Batch Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Select All / Deselect All
            Box(
                modifier = Modifier
                    .size(Dimensions.ActionButtonSize)
                    .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                    .background(if (isAllSelected) AppColors.brandAccentSubtle else AppColors.cardSurfaceSubtle)
                    .border(
                        width = Dimensions.CardBorderWidth,
                        color = if (isAllSelected) AppColors.brandAccent else AppColors.cardStroke,
                        shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
                    )
                    .clickable { onToggleSelectAll() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_select_all),
                    contentDescription = stringResource(
                        if (isAllSelected) R.string.deselect_all else R.string.select_all
                    ),
                    tint = if (isAllSelected) AppColors.brandAccent else AppColors.textSecondary,
                    modifier = Modifier.size(Dimensions.IconSizeMedium)
                )
            }

            // Copy
            if (selectedCount > 0) {
                Box(
                    modifier = Modifier
                        .size(Dimensions.ActionButtonSize)
                        .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                        .background(AppColors.cardSurfaceSubtle)
                        .border(
                            width = Dimensions.CardBorderWidth,
                            color = AppColors.cardStroke,
                            shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
                        )
                        .clickable { onBatchCopy() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_copy),
                        contentDescription = stringResource(R.string.action_copy),
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(Dimensions.IconSizeMedium)
                    )
                }

                // Cut
                Box(
                    modifier = Modifier
                        .size(Dimensions.ActionButtonSize)
                        .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                        .background(AppColors.cardSurfaceSubtle)
                        .border(
                            width = Dimensions.CardBorderWidth,
                            color = AppColors.cardStroke,
                            shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
                        )
                        .clickable { onBatchCut() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_cut),
                        contentDescription = stringResource(R.string.action_cut),
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(Dimensions.IconSizeMedium)
                    )
                }

                // Delete / Trash
                Box(
                    modifier = Modifier
                        .size(Dimensions.ActionButtonSize)
                        .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                        .background(AppColors.cardSurfaceSubtle)
                        .border(
                            width = Dimensions.CardBorderWidth,
                            color = AppColors.cardStroke,
                            shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
                        )
                        .clickable { onBatchDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.action_delete),
                        tint = AppColors.statusCritical,
                        modifier = Modifier.size(Dimensions.IconSizeMedium)
                    )
                }
            }
        }
    }
}
