package tr.canugur.mywayfiles.ui.trash.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.TrashedItem
import tr.canugur.mywayfiles.ui.theme.AppColors

/**
 * Confirmation dialog for emptying the entire Recycle Bin.
 */
@Composable
fun EmptyTrashConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.cardSurface,
        title = {
            Text(
                text = stringResource(R.string.action_empty_recycle_bin),
                color = AppColors.statusCritical,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_empty_recycle_bin_confirm),
                color = AppColors.textPrimary,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.action_empty_recycle_bin),
                    color = AppColors.statusCritical
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = AppColors.textSecondary
                )
            }
        }
    )
}

/**
 * Confirmation dialog for permanently deleting a specific file or folder.
 */
@Composable
fun PermanentDeleteConfirmDialog(
    item: TrashedItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.cardSurface,
        title = {
            Text(
                text = stringResource(R.string.action_delete_permanently),
                color = AppColors.statusCritical,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_delete_confirm, item.name),
                color = AppColors.textPrimary,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.action_delete),
                    color = AppColors.statusCritical
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = AppColors.textSecondary
                )
            }
        }
    )
}
