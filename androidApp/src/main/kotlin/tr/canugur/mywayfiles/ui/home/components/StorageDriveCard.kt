package tr.canugur.mywayfiles.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.domain.model.StorageDrive
import tr.canugur.mywayfiles.domain.model.StorageDriveType
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions
import tr.canugur.mywayfiles.ui.theme.MotionTokens
import tr.canugur.mywayfiles.ui.theme.StorageThresholds

/**
 * Drive tile component visualizing volume capacity and health.
 */
@Composable
fun StorageDriveCard(
    drive: StorageDrive,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val iconRes = when (drive.type) {
        StorageDriveType.INTERNAL -> R.drawable.ic_drive_disk
        StorageDriveType.SD_CARD -> R.drawable.ic_drive_sdcard
        StorageDriveType.USB -> R.drawable.ic_drive_usb
    }

    val typeLabelRes = when (drive.type) {
        StorageDriveType.INTERNAL -> R.string.type_internal
        StorageDriveType.SD_CARD -> R.string.type_sdcard
        StorageDriveType.USB -> R.string.type_usb
    }

    val animatedProgress by animateFloatAsState(
        targetValue = drive.usageRatio,
        animationSpec = tween(durationMillis = MotionTokens.DURATION_DEFAULT_MS),
        label = "driveUsageProgressAnimation"
    )

    Surface(
        color = AppColors.CardSurface,
        shape = RoundedCornerShape(Dimensions.CornerRadiusDefault),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = Dimensions.CardBorderWidth,
                color = AppColors.CardStroke,
                shape = RoundedCornerShape(Dimensions.CornerRadiusDefault)
            )
            .clip(RoundedCornerShape(Dimensions.CornerRadiusDefault))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(Dimensions.SpacingDefault),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drive Icon Container
            Box(
                modifier = Modifier
                    .size(Dimensions.IconContainerSize)
                    .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                    .background(AppColors.CardSurfaceSubtle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = drive.name,
                    tint = AppColors.BrandAccent,
                    modifier = Modifier.size(Dimensions.IconSizeLarge)
                )
            }

            Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

            // Drive Metrics & Progress Bar
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Name & Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = drive.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))

                    Text(
                        text = stringResource(typeLabelRes),
                        fontSize = 11.sp,
                        color = AppColors.TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))

                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimensions.ProgressBarHeight)
                        .clip(RoundedCornerShape(Dimensions.ProgressBarRadius))
                        .background(AppColors.ProgressTrack)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(Dimensions.ProgressBarRadius))
                            .background(
                                when {
                                    drive.usageRatio >= StorageThresholds.CRITICAL_RATIO -> AppColors.StatusCritical
                                    drive.usageRatio >= StorageThresholds.WARNING_RATIO -> AppColors.StatusWarning
                                    else -> AppColors.BrandAccent
                                }
                            )
                    )
                }

                Spacer(modifier = Modifier.height(Dimensions.SpacingExtraSmall))

                // Standard Format: "X free of Y"
                Text(
                    text = stringResource(
                        R.string.free_of_total,
                        ByteFormatter.format(drive.freeBytes),
                        ByteFormatter.format(drive.totalBytes)
                    ),
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}
