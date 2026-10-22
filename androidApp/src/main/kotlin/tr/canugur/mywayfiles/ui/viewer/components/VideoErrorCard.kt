package tr.canugur.mywayfiles.ui.viewer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.theme.Dimensions
import tr.canugur.mywayfiles.ui.theme.AppColors

/**
 * Crash-proof error state card when video decoding or format playback fails.
 */
@Composable
fun VideoErrorCard(
    item: FileItem,
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.mediaCanvas.copy(alpha = 0.92f))
            .padding(Dimensions.SpacingLarge),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimensions.CornerRadiusMedium))
                .background(AppColors.mediaSurface)
                .border(
                    width = Dimensions.CardBorderWidth,
                    color = AppColors.mediaError.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(Dimensions.CornerRadiusMedium)
                )
                .padding(Dimensions.SpacingLarge)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_video),
                contentDescription = null,
                tint = AppColors.mediaError,
                modifier = Modifier.size(Dimensions.IconSizeLarge)
            )
            Text(
                text = item.name,
                color = AppColors.mediaTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
            Text(
                text = errorMessage,
                color = AppColors.mediaErrorMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.mediaAccent,
                    contentColor = AppColors.mediaAccentContent
                ),
                shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
            ) {
                Text(text = stringResource(R.string.action_retry), fontSize = 14.sp)
            }
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.mediaSurfaceRaised,
                    contentColor = AppColors.mediaTextPrimary
                ),
                shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
            ) {
                Text(text = stringResource(R.string.close_viewer), fontSize = 14.sp)
            }
        }
    }
}
