package tr.canugur.mywayfiles.ui.home.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * Empty state view when no physical drives are mounted.
 */
@Composable
fun EmptyDrivesView(
    modifier: Modifier = Modifier
) {
    Surface(
        color = AppColors.cardSurface,
        shape = RoundedCornerShape(Dimensions.CornerRadiusDefault),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = Dimensions.CardBorderWidth,
                color = AppColors.cardStroke,
                shape = RoundedCornerShape(Dimensions.CornerRadiusDefault)
            )
            .padding(vertical = Dimensions.SpacingHuge, horizontal = Dimensions.SpacingDefault)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_drive_disk),
                contentDescription = null,
                tint = AppColors.textMuted,
                modifier = Modifier.size(Dimensions.IconSizeLarge)
            )
            Spacer(modifier = Modifier.height(Dimensions.SpacingMedium))
            Text(
                text = stringResource(R.string.no_drives_found),
                fontSize = 13.sp,
                color = AppColors.textSecondary
            )
        }
    }
}
