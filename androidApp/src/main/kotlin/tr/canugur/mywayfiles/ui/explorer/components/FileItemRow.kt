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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.core.util.DateTimeFormatter
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * Enterprise list item rendering a directory or file entry with precise metadata.
 */
@Composable
fun FileItemRow(
    item: FileItem,
    onClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(Dimensions.CornerRadiusDefault)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.cardSurface)
            .border(
                width = Dimensions.CardBorderWidth,
                color = AppColors.cardStroke,
                shape = shape
            )
            .clickable { onClick(item) }
            .padding(
                horizontal = Dimensions.SpacingMedium,
                vertical = Dimensions.SpacingSmall
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading Thumbnail or Vector Icon
        FileThumbnailView(item = item)

        Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

        // Name and Metadata
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.name,
                fontSize = 13.sp,
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
                    fontSize = 11.sp,
                    color = AppColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Extension Badge for files
        if (!item.isDirectory && item.extension.isNotEmpty()) {
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
                    text = item.extension.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textSecondary
                )
            }
        }
    }
}
