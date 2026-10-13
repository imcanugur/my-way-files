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
 * Navigation top bar featuring title breadcrumbs, theme switcher, and refresh controls.
 */
@Composable
fun HomeTopBar(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onRefreshClick: () -> Unit,
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
                    color = AppColors.textPrimary
                )
                Text(
                    text = ">",
                    fontSize = 14.sp,
                    color = AppColors.textMuted
                )
                Text(
                    text = stringResource(R.string.devices_and_drives),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.textSecondary
                )
            }

            // Action Buttons: Theme Toggle & Refresh
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Theme Switcher Toggle Button
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
                        .clickable { onToggleTheme() },
                    contentAlignment = Alignment.Center
                ) {
                    val themeIcon = if (isDarkTheme) R.drawable.ic_mode_light else R.drawable.ic_mode_dark
                    Icon(
                        painter = painterResource(themeIcon),
                        contentDescription = stringResource(R.string.theme_toggle),
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(Dimensions.IconSizeMedium)
                    )
                }

                // Refresh Action Button
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
                        .clickable { onRefreshClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = stringResource(R.string.refresh_drives),
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(Dimensions.IconSizeMedium)
                    )
                }
            }
        }
    }
}
