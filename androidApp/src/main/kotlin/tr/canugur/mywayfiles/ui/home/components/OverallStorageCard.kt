package tr.canugur.mywayfiles.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.domain.model.StorageSummary
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions
import tr.canugur.mywayfiles.ui.theme.MotionTokens
import tr.canugur.mywayfiles.ui.theme.StorageThresholds

/**
 * Storage summary card visualizing total device storage metrics.
 */
@Composable
fun OverallStorageCard(
    summary: StorageSummary,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = summary.usageRatio,
        animationSpec = tween(durationMillis = MotionTokens.DURATION_DEFAULT_MS),
        label = "overallStorageProgressAnimation"
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
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.SpacingDefault)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.storage_overview_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = "${summary.driveCount} ${stringResource(R.string.devices_and_drives).lowercase()}",
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))

            // Thin Horizontal Progress Bar
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
                                summary.usageRatio >= StorageThresholds.CRITICAL_RATIO -> AppColors.StatusCritical
                                summary.usageRatio >= StorageThresholds.WARNING_RATIO -> AppColors.StatusWarning
                                else -> AppColors.BrandAccent
                            }
                        )
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(
                        R.string.free_of_total,
                        ByteFormatter.format(summary.freeBytes),
                        ByteFormatter.format(summary.totalBytes)
                    ),
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
                Text(
                    text = "%${(summary.usageRatio * 100).toInt()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (summary.usageRatio >= StorageThresholds.CRITICAL_RATIO) {
                        AppColors.StatusCritical
                    } else {
                        AppColors.TextPrimary
                    }
                )
            }
        }
    }
}
