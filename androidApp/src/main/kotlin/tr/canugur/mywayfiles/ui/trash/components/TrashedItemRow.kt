package tr.canugur.mywayfiles.ui.trash.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.core.util.DateTimeFormatter
import tr.canugur.mywayfiles.domain.model.TrashedItem
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * List item row for a deleted file or folder in the Recycle Bin.
 */
@Composable
fun TrashedItemRow(
    item: TrashedItem,
    onClick: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(Dimensions.CornerRadiusDefault)
    val extension = remember(item.name) {
        item.name.substringAfterLast('.', "").lowercase()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.cardSurface)
            .border(width = Dimensions.CardBorderWidth, color = AppColors.cardStroke, shape = shape)
            .clickable { onClick() }
            .padding(horizontal = Dimensions.SpacingMedium, vertical = Dimensions.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // High-performance cached thumbnail or folder/file icon
        TrashedItemThumbnailView(item = item)

        Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(Dimensions.SpacingMicro))

            Text(
                text = "${stringResource(R.string.original_location)}: ${item.originalPath}",
                fontSize = 11.sp,
                color = AppColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${ByteFormatter.format(item.sizeBytes)} • ${DateTimeFormatter.format(item.deletedAtMs)}",
                fontSize = 10.sp,
                color = AppColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Extension Badge
        if (!item.isDirectory && extension.isNotEmpty()) {
            Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                    .background(AppColors.cardSurfaceSubtle)
                    .border(
                        width = Dimensions.CardBorderWidth,
                        color = AppColors.cardStroke,
                        shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
                    )
                    .padding(
                        horizontal = Dimensions.SpacingExtraSmall,
                        vertical = Dimensions.SpacingMicro
                    )
            ) {
                Text(
                    text = extension.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))

        // Actions: Restore & Permanent Delete
        Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMicro)) {
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
