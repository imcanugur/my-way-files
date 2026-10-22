package tr.canugur.mywayfiles.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import tr.canugur.mywayfiles.domain.model.StorageDrive
import tr.canugur.mywayfiles.domain.model.StorageDriveType
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions
import tr.canugur.mywayfiles.ui.theme.MotionTokens
import tr.canugur.mywayfiles.ui.theme.StorageThresholds

/** Compact, content-first drive row; one row is one navigation target. */
@Composable
fun StorageDriveCard(drive: StorageDrive, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
    val progress by animateFloatAsState(
        targetValue = drive.usageRatio.coerceIn(0f, 1f),
        animationSpec = tween(MotionTokens.DURATION_DEFAULT_MS),
        label = "driveUsageProgress"
    )
    val usageColor = when {
        drive.usageRatio >= StorageThresholds.CRITICAL_RATIO -> AppColors.statusCritical
        drive.usageRatio >= StorageThresholds.WARNING_RATIO -> AppColors.statusWarning
        else -> AppColors.brandAccent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.CornerRadiusMedium))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Dimensions.SpacingMedium, horizontal = Dimensions.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = AppColors.cardSurfaceSubtle,
            modifier = Modifier.size(Dimensions.IconContainerSize)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = AppColors.brandAccent,
                    modifier = Modifier.size(Dimensions.IconSizeDefault)
                )
            }
        }
        Spacer(Modifier.width(Dimensions.SpacingDefault))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = drive.name,
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(Dimensions.SpacingMicro))
            Text(
                text = stringResource(
                    R.string.drive_summary,
                    stringResource(typeLabelRes),
                    ByteFormatter.format(drive.freeBytes),
                    ByteFormatter.format(drive.totalBytes)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(Dimensions.SpacingSmall))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.SpacingExtraSmall)
                    .clip(RoundedCornerShape(Dimensions.CornerRadiusPill))
                    .background(AppColors.progressTrack)
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(Dimensions.SpacingExtraSmall)
                        .background(usageColor)
                )
            }
        }
        Spacer(Modifier.width(Dimensions.SpacingMedium))
        Text(
            text = stringResource(R.string.storage_usage_percentage, (drive.usageRatio * 100).toInt()),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}
