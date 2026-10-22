package tr.canugur.mywayfiles.ui.trash.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/** Focused top app bar for review and recovery of deleted items. */
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
        color = AppColors.canvasBackground,
        modifier = modifier.fillMaxWidth().statusBarsPadding()
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.SpacingSmall, vertical = Dimensions.SpacingSmall)
        ) {
          val useCompactAction = maxWidth < Dimensions.CompactActionBreakpoint
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.size(Dimensions.ActionButtonSize)) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.navigate_up),
                    tint = AppColors.textPrimary
                )
            }
            Spacer(Modifier.width(Dimensions.SpacingSmall))
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.recycle_bin),
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.textPrimary
                )
                if (hasItems) {
                    Text(
                        text = stringResource(R.string.recycle_bin_item_summary, itemCount, ByteFormatter.format(totalBytes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textSecondary
                    )
                }
            }
            if (hasItems) {
                if (useCompactAction) {
                    IconButton(onClick = onEmptyTrashClick, modifier = Modifier.size(Dimensions.ActionButtonSize)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_empty_trash),
                            contentDescription = stringResource(R.string.action_empty_recycle_bin),
                            tint = AppColors.statusCritical
                        )
                    }
                } else {
                    Button(
                        onClick = onEmptyTrashClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.statusCritical.copy(alpha = 0.12f),
                            contentColor = AppColors.statusCritical
                        ),
                        shape = RoundedCornerShape(Dimensions.CornerRadiusLarge)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_empty_trash),
                            contentDescription = null,
                            modifier = Modifier.size(Dimensions.IconSizeMedium)
                        )
                        Spacer(Modifier.width(Dimensions.SpacingSmall))
                        Text(stringResource(R.string.action_empty_recycle_bin))
                    }
                }
            }
          }
        }
    }
}
