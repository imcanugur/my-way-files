package tr.canugur.mywayfiles.ui.viewer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import tr.canugur.mywayfiles.core.util.FileTypeClassifier
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.theme.Dimensions
import tr.canugur.mywayfiles.ui.theme.AppColors

/**
 * Unified top bar overlay for media viewers, displaying item title, metadata, and playlist counter.
 */
@Composable
fun ViewerTopBar(
    item: FileItem,
    currentIndex: Int,
    totalCount: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AppColors.mediaControlSurfaceStrong,
        shape = RoundedCornerShape(Dimensions.CornerRadiusLarge),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Dimensions.SpacingDefault, vertical = Dimensions.SpacingSmall)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Dimensions.SpacingSmall,
                    vertical = Dimensions.SpacingExtraSmall
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.close_viewer),
                    tint = AppColors.mediaTextPrimary,
                    modifier = Modifier.size(Dimensions.IconSizeMedium)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Dimensions.SpacingSmall)
            ) {
                Text(
                    text = item.name,
                    color = AppColors.mediaTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val videoTypeLabel = if (FileTypeClassifier.isVideo(item)) {
                    stringResource(R.string.media_type_video)
                } else null
                val meta = buildString {
                    append(ByteFormatter.format(item.sizeBytes))
                    val dateStr = DateTimeFormatter.format(item.lastModifiedMs)
                    if (dateStr.isNotEmpty()) append(" • $dateStr")
                    videoTypeLabel?.let { append(" • $it") }
                }

                Text(
                    text = meta,
                    color = AppColors.mediaTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (totalCount > 1) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Dimensions.CornerRadiusPill))
                        .background(AppColors.mediaSurfaceRaised)
                        .padding(
                            horizontal = Dimensions.SpacingMedium,
                            vertical = Dimensions.SpacingExtraSmall
                        )
                ) {
                    Text(
                        text = "${currentIndex + 1} / $totalCount",
                        color = AppColors.mediaTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
