package tr.canugur.mywayfiles.ui.home.components

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
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * Desktop-grade Quick Access card navigating to the Recycle Bin.
 */
@Composable
fun RecycleBinCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(Dimensions.CornerRadiusMedium)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.cardSurface)
            .border(width = Dimensions.CardBorderWidth, color = AppColors.cardStroke, shape = shape)
            .clickable { onClick() }
            .padding(horizontal = Dimensions.SpacingDefault, vertical = Dimensions.SpacingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Dimensions.IconContainerSize)
                .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                .background(AppColors.cardSurfaceSubtle)
                .border(
                    width = Dimensions.CardBorderWidth,
                    color = AppColors.cardStroke,
                    shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_recycle_bin),
                contentDescription = null,
                tint = AppColors.brandAccent,
                modifier = Modifier.size(Dimensions.IconSizeDefault)
            )
        }

        Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.recycle_bin),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary
            )
            Spacer(modifier = Modifier.height(Dimensions.SpacingMicro))
            Text(
                text = stringResource(R.string.recycle_bin),
                fontSize = 12.sp,
                color = AppColors.textMuted
            )
        }

        Text(
            text = ">",
            fontSize = 14.sp,
            color = AppColors.textMuted,
            fontWeight = FontWeight.Bold
        )
    }
}
