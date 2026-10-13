package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.ui.explorer.Breadcrumb
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * Explorer Top Bar with back navigation, breadcrumb path bar, new folder creation,
 * theme toggle, and refresh.
 */
@Composable
fun ExplorerTopBar(
    breadcrumbs: List<Breadcrumb>,
    isDarkTheme: Boolean,
    onBackClick: () -> Unit,
    onBreadcrumbClick: (String) -> Unit,
    onCreateFolderClick: () -> Unit,
    onToggleTheme: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(breadcrumbs.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Surface(
        color = AppColors.cardSurface,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .border(width = Dimensions.CardBorderWidth, color = AppColors.cardStroke)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Dimensions.SpacingDefault,
                    vertical = Dimensions.SpacingSmall
                ),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
        ) {
            // Main Top Bar Row: Back Button, Title, and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button + Current Folder Name
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
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_up),
                            tint = AppColors.textPrimary,
                            modifier = Modifier.size(Dimensions.IconSizeMedium)
                        )
                    }

                    Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))

                    val activeTitle = breadcrumbs.lastOrNull()?.title ?: ""
                    Text(
                        text = activeTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Actions: New Folder, Theme Switcher & Refresh
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
                    verticalAlignment = Alignment.CenterVertically
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
                            .clickable { onCreateFolderClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_new_folder),
                            contentDescription = stringResource(R.string.action_new_folder),
                            tint = AppColors.brandAccent,
                            modifier = Modifier.size(Dimensions.IconSizeMedium)
                        )
                    }

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

            // Desktop-style Address Bar (Breadcrumbs)
            val addressBarShape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(addressBarShape)
                    .background(AppColors.cardSurfaceSubtle)
                    .border(
                        width = Dimensions.CardBorderWidth,
                        color = AppColors.cardStroke,
                        shape = addressBarShape
                    )
                    .padding(
                        horizontal = Dimensions.SpacingSmall,
                        vertical = Dimensions.SpacingExtraSmall
                    )
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(scrollState),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    breadcrumbs.forEachIndexed { index, crumb ->
                        val isLast = index == breadcrumbs.lastIndex

                        Text(
                            text = crumb.title,
                            fontSize = 12.sp,
                            fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isLast) AppColors.brandAccent else AppColors.textSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                                .clickable(enabled = !isLast) { onBreadcrumbClick(crumb.path) }
                                .padding(
                                    horizontal = Dimensions.SpacingExtraSmall,
                                    vertical = Dimensions.SpacingMicro
                                )
                        )

                        if (!isLast) {
                            Text(
                                text = ">",
                                fontSize = 11.sp,
                                color = AppColors.textMuted,
                                modifier = Modifier.padding(horizontal = Dimensions.SpacingMicro)
                            )
                        }
                    }
                }
            }
        }
    }
}
