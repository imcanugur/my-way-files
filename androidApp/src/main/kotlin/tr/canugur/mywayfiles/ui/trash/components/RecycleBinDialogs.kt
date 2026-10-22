package tr.canugur.mywayfiles.ui.trash.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.TrashedItem
import tr.canugur.mywayfiles.ui.common.AppModalBottomSheet
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

@Composable
fun EmptyTrashConfirmationSheet(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AppModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.SpacingLarge)
                .padding(bottom = Dimensions.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
        ) {
            Text(stringResource(R.string.action_empty_recycle_bin), color = AppColors.statusCritical,
                style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.dialog_empty_recycle_bin_confirm), color = AppColors.textSecondary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.action_empty_recycle_bin), color = AppColors.statusCritical)
                }
            }
        }
    }
}

@Composable
fun PermanentDeleteConfirmationSheet(item: TrashedItem, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AppModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.SpacingLarge)
                .padding(bottom = Dimensions.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
        ) {
            Text(stringResource(R.string.action_delete_permanently), color = AppColors.statusCritical,
                style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.dialog_delete_confirm, item.name), color = AppColors.textSecondary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.action_delete), color = AppColors.statusCritical)
                }
            }
        }
    }
}
