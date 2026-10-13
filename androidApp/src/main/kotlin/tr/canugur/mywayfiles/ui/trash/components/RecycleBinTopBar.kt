package tr.canugur.mywayfiles.ui.trash.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * Top bar header for Recycle Bin featuring back navigation, title, item counter, and empty trash trigger.
 */
@Composable
fun RecycleBinTopBar(
    itemCount: Int,
    totalBytes: Long,
    hasItems: Boolean,
    onNavigateBack: () -> Unit,
    onEmptyTrashClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AppColors.cardSurface,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .border(width = Dimensions.CardBorderWidth, color = AppColors.cardStroke)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SpacingDefault, vertical = Dimensions.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.navigate_up),
                        tint = AppColors.textPrimary,
                        modifier = Modifier.size(Dimensions.IconSizeMedium)
                    )
                }

                Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

                Column {
                    Text(
                        text = stringResource(R.string.recycle_bin),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimary
                    )

                    if (hasItems) {
                        Text(
                            text = "$itemCount öge • ${ByteFormatter.format(totalBytes)}",
                            fontSize = 11.sp,
                            color = AppColors.textMuted
                        )
                    }
                }
            }

            if (hasItems) {
                Button(
                    onClick = onEmptyTrashClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.statusCritical.copy(alpha = 0.12f),
                        contentColor = AppColors.statusCritical
                    ),
                    shape = RoundedCornerShape(Dimensions.CornerRadiusSmall),
                    contentPadding = PaddingValues(
                        horizontal = Dimensions.SpacingSmall,
                        vertical = Dimensions.SpacingExtraSmall
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_empty_trash),
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.IconSizeSmall)
                    )
                    Spacer(modifier = Modifier.width(Dimensions.SpacingExtraSmall))
                    Text(
                        text = stringResource(R.string.action_empty_recycle_bin),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
