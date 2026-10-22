package tr.canugur.mywayfiles.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.domain.model.StorageSummary
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions
import tr.canugur.mywayfiles.ui.theme.MotionTokens
import tr.canugur.mywayfiles.domain.policy.StorageThresholds

/** Storage at a glance, using the real totals supplied by the storage repository. */
@Composable
fun OverallStorageCard(summary: StorageSummary, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(
        targetValue = summary.usageRatio.coerceIn(0f, 1f),
        animationSpec = tween(MotionTokens.DURATION_DEFAULT_MS),
        label = "overallStorageProgress"
    )
    val usageColor = when {
        summary.usageRatio >= StorageThresholds.CRITICAL_RATIO -> AppColors.statusCritical
        summary.usageRatio >= StorageThresholds.WARNING_RATIO -> AppColors.statusWarning
        else -> AppColors.brandAccent
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AppColors.brandAccentSubtle,
        shape = RoundedCornerShape(Dimensions.CornerRadiusLarge)
    ) {
        Column(modifier = Modifier.padding(Dimensions.SpacingLarge)) {
            Text(
                text = stringResource(R.string.storage_overview_title),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.textPrimary
            )
            Spacer(Modifier.height(Dimensions.SpacingMedium))
            Text(
                text = stringResource(R.string.free_of_total, ByteFormatter.format(summary.freeBytes), ByteFormatter.format(summary.totalBytes)),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary
            )
            Spacer(Modifier.height(Dimensions.SpacingDefault))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.ProgressBarHeight)
                    .clip(RoundedCornerShape(Dimensions.CornerRadiusPill))
                    .background(AppColors.progressTrack),
                horizontalArrangement = Arrangement.Start
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(Dimensions.CornerRadiusPill))
                        .background(usageColor)
                )
            }
            Spacer(Modifier.height(Dimensions.SpacingSmall))
            Text(
                text = stringResource(R.string.storage_usage_percentage, (summary.usageRatio * 100).toInt()),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = AppColors.textSecondary
            )
        }
    }
}
