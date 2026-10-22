package tr.canugur.mywayfiles.ui.trash.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
@Composable
fun TrashedItemRow(
    item: TrashedItem,
    onClick: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(Dimensions.CornerRadiusDefault)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Dimensions.SpacingSmall, vertical = Dimensions.SpacingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
