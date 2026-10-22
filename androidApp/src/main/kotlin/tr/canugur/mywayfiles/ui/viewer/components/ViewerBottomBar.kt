package tr.canugur.mywayfiles.ui.viewer.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.theme.Dimensions
import tr.canugur.mywayfiles.ui.theme.AppColors

/**
 * Reusable quick-action bottom toolbar for media viewers (Share, Copy, Properties, Delete).
 */
@Composable
fun ViewerBottomBar(
    item: FileItem,
    onShare: (FileItem) -> Unit,
    onCopy: (FileItem) -> Unit,
    onMoveToFolder: (FileItem) -> Unit,
    onProperties: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AppColors.mediaControlSurfaceStrong,
        shape = RoundedCornerShape(Dimensions.CornerRadiusLarge),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Dimensions.SpacingDefault, vertical = Dimensions.SpacingSmall)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = Dimensions.SpacingSmall,
                    horizontal = Dimensions.SpacingMedium
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ViewerActionItem(
                iconRes = R.drawable.ic_share,
                label = stringResource(R.string.action_share),
                tint = AppColors.mediaTextPrimary,
                onClick = { onShare(item) }
            )

            ViewerActionItem(
                iconRes = R.drawable.ic_copy,
                label = stringResource(R.string.action_copy),
                tint = AppColors.mediaTextPrimary,
                onClick = { onCopy(item) }
            )

            ViewerActionItem(
                iconRes = R.drawable.ic_folder,
                label = stringResource(R.string.action_move),
                tint = AppColors.mediaTextPrimary,
                onClick = { onMoveToFolder(item) }
            )

            ViewerActionItem(
                iconRes = R.drawable.ic_info,
                label = stringResource(R.string.action_properties),
                tint = AppColors.mediaTextPrimary,
                onClick = { onProperties(item) }
            )

            ViewerActionItem(
                iconRes = R.drawable.ic_delete,
                label = stringResource(R.string.action_delete),
                tint = AppColors.mediaError,
                onClick = { onDelete(item) }
            )
        }
    }
}

@Composable
fun ViewerActionItem(
    iconRes: Int,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
            .heightIn(min = Dimensions.ActionButtonSize)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(
                horizontal = Dimensions.SpacingMedium,
                vertical = Dimensions.SpacingExtraSmall
            )
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(Dimensions.IconSizeMedium)
        )
        Spacer(modifier = Modifier.height(Dimensions.SpacingMicro))
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
