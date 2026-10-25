package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/** Direct selection actions in a compact pill; secondary actions scroll horizontally instead of hiding behind overflow. */
@Composable
fun ExplorerSelectionActionBar(
    selectedCount: Int,
    canShare: Boolean,
    canMove: Boolean,
    canRename: Boolean,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onProperties: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = Dimensions.SpacingSmall),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier.widthIn(max = maxWidth - Dimensions.SpacingLarge),
            color = AppColors.cardSurface,
            contentColor = AppColors.textPrimary,
            shape = RoundedCornerShape(Dimensions.CornerRadiusPill),
            shadowElevation = 4.dp,
            tonalElevation = 2.dp,
            border = BorderStroke(Dimensions.CardBorderWidth, AppColors.cardStroke)
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState())
                    .padding(horizontal = Dimensions.SpacingSmall, vertical = Dimensions.SpacingExtraSmall),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingExtraSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionAction(R.string.action_cut, R.drawable.ic_cut, onCut, selectedCount > 0)
                SelectionAction(R.string.action_copy, R.drawable.ic_copy, onCopy, selectedCount > 0)
                if (canMove) SelectionAction(R.string.action_move_to_folder, R.drawable.ic_folder, onMove, selectedCount > 0)
                if (canShare) SelectionAction(R.string.action_share, R.drawable.ic_share, onShare, selectedCount > 0)
                if (canRename) SelectionAction(R.string.action_rename, R.drawable.ic_edit_rename, onRename, selectedCount > 0)
                SelectionAction(R.string.action_delete, R.drawable.ic_delete, onDelete, selectedCount > 0, AppColors.statusCritical)
                if (selectedCount == 1) SelectionAction(R.string.action_properties, R.drawable.ic_info, onProperties, true)
            }
        }
    }
}

@Composable
private fun SelectionAction(
    label: Int,
    icon: Int,
    onClick: () -> Unit,
    enabled: Boolean,
    tint: androidx.compose.ui.graphics.Color = AppColors.textSecondary
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(Dimensions.ActionButtonSize)) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(label),
            tint = tint,
            modifier = Modifier.size(Dimensions.IconSizeMedium)
        )
    }
}
