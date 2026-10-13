package tr.canugur.mywayfiles.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
 * Clean navigation top bar following structured design tokens.
 */
@Composable
fun HomeTopBar(
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AppColors.CardSurface,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .border(width = Dimensions.CardBorderWidth, color = AppColors.CardStroke)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SpacingLarge, vertical = Dimensions.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Breadcrumb Navigation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = ">",
                    fontSize = 14.sp,
                    color = AppColors.TextMuted
                )
                Text(
                    text = stringResource(R.string.devices_and_drives),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextSecondary
                )
            }

            // Action Button
            Box(
                modifier = Modifier
                    .size(Dimensions.ActionButtonSize)
                    .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                    .background(AppColors.CardSurfaceSubtle)
                    .border(
                        width = Dimensions.CardBorderWidth,
                        color = AppColors.CardStroke,
                        shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
                    )
                    .clickable { onRefreshClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = stringResource(R.string.refresh_drives),
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(Dimensions.IconSizeMedium)
                )
            }
        }
    }
}
