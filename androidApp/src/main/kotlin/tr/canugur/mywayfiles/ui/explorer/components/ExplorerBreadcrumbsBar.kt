package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.ui.explorer.Breadcrumb
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * Horizontally scrollable, interactive breadcrumb trail enabling 1-tap navigation
 * up any number of parent directory levels.
 */
@Composable
fun ExplorerBreadcrumbsBar(
    breadcrumbs: List<Breadcrumb>,
    onBreadcrumbClick: (String) -> Unit,
    isLoading: Boolean = false,
    loadedItemCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val loadingDescription = if (isLoading) {
        stringResource(R.string.folder_scan_progress, loadedItemCount)
    } else null
    val scrollState = rememberScrollState()
    LaunchedEffect(breadcrumbs.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Surface(
        color = AppColors.canvasBackground,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SpacingLarge, vertical = Dimensions.SpacingExtraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMicro)
            ) {
                breadcrumbs.forEachIndexed { index, crumb ->
                val isLast = index == breadcrumbs.lastIndex

                if (isLast) {
                    Text(
                        text = crumb.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.brandAccent,
                        modifier = Modifier.padding(
                            horizontal = Dimensions.SpacingSmall,
                            vertical = Dimensions.SpacingMicro
                        )
                    )
                } else {
                    Text(
                        text = crumb.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.textSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                            .clickable { onBreadcrumbClick(crumb.path) }
                            .padding(
                                horizontal = Dimensions.SpacingSmall,
                                vertical = Dimensions.SpacingMicro
                            )
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = AppColors.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
                }
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(start = Dimensions.SpacingSmall)
                        .size(14.dp)
                        .semantics {
                            contentDescription = loadingDescription.orEmpty()
                        },
                    color = AppColors.brandAccent,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
