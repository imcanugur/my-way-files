package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.ui.explorer.Clipboard
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * Ergonomic floating capsule bar shown in the thumb zone when items are in clipboard.
 * Tapping opens the standard [ClipboardConfirmationBottomSheet] for destination review and paste confirmation.
 */
@Composable
fun ClipboardBar(
    clipboard: Clipboard,
    destinationPath: String,
    destinationName: String,
    isOperating: Boolean = false,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmationSheet by remember { mutableStateOf(false) }

    Surface(
        color = AppColors.cardSurface,
        contentColor = AppColors.textPrimary,
        shape = RoundedCornerShape(Dimensions.CornerRadiusPill),
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, AppColors.cardStroke),
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = Dimensions.SpacingMedium)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
        ) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.action_cancel),
                    tint = AppColors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            VerticalDivider(
                modifier = Modifier.height(24.dp),
                color = AppColors.divider
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimensions.CornerRadiusPill))
                    .clickable(enabled = !isOperating) {
                        showConfirmationSheet = true
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(if (clipboard.isCut) R.drawable.ic_cut else R.drawable.ic_copy),
                    contentDescription = null,
                    tint = AppColors.brandAccent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(
                        if (clipboard.isCut) R.string.action_move_here else R.string.action_copy_here
                    ) + " (${clipboard.items.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.brandAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showConfirmationSheet) {
        ClipboardConfirmationBottomSheet(
            clipboard = clipboard,
            destinationPath = destinationPath,
            destinationName = destinationName,
            isOperating = isOperating,
            onConfirm = {
                showConfirmationSheet = false
                onPaste()
            },
            onDismiss = {
                showConfirmationSheet = false
            }
        )
    }
}
