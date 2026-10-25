package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.core.util.DateTimeFormatter
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.common.AppModalBottomSheet
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

@Composable
fun RenameDialog(initialName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initialName) }
    AppModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.SpacingLarge)
                .padding(bottom = Dimensions.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
        ) {
            Text(stringResource(R.string.dialog_rename_title), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.brandAccent,
                    unfocusedBorderColor = AppColors.cardStroke,
                    focusedTextColor = AppColors.textPrimary,
                    unfocusedTextColor = AppColors.textPrimary
                )
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                TextButton(onClick = {
                    if (text.isNotBlank() && text != initialName) onConfirm(text.trim())
                    onDismiss()
                }) { Text(stringResource(R.string.action_confirm), color = AppColors.brandAccent) }
            }
        }
    }
}

@Composable
fun CreateFolderDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AppModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.SpacingLarge)
                .padding(bottom = Dimensions.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
        ) {
            Text(stringResource(R.string.dialog_new_folder_title), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(stringResource(R.string.dialog_new_folder_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.brandAccent,
                    unfocusedBorderColor = AppColors.cardStroke,
                    focusedTextColor = AppColors.textPrimary,
                    unfocusedTextColor = AppColors.textPrimary
                )
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                TextButton(onClick = {
                    if (text.isNotBlank()) onConfirm(text.trim())
                    onDismiss()
                }) { Text(stringResource(R.string.action_confirm), color = AppColors.brandAccent) }
            }
        }
    }
}

@Composable
fun DeleteConfirmDialog(fileName: String, permanentlyDelete: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AppModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.SpacingLarge)
                .padding(bottom = Dimensions.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
        ) {
            Text(
                stringResource(
                    if (permanentlyDelete) R.string.dialog_permanent_delete_title
                    else R.string.dialog_move_to_trash_title
                ),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                stringResource(
                    if (permanentlyDelete) R.string.dialog_permanent_delete_confirm
                    else R.string.dialog_move_to_trash_confirm,
                    fileName
                ),
                color = AppColors.textSecondary
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                Button(
                    onClick = { onConfirm(); onDismiss() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.statusCritical,
                        contentColor = AppColors.textPrimary
                    ),
                    shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
                ) {
                    Text(
                        stringResource(
                            if (permanentlyDelete) R.string.action_delete_permanently
                            else R.string.action_move_to_trash
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun PropertiesDialog(item: FileItem, onDismiss: () -> Unit) {
    AppModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp).verticalScroll(rememberScrollState())
                .padding(horizontal = Dimensions.SpacingLarge)
                .padding(bottom = Dimensions.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
        ) {
            Text(
                item.name,
                color = AppColors.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)) {
                PropertyRow(stringResource(R.string.properties_path), item.path)
                PropertyRow(
                    stringResource(R.string.properties_size),
                    if (item.isDirectory) stringResource(R.string.items_count, item.childCount ?: 0)
                    else ByteFormatter.format(item.sizeBytes)
                )
                PropertyRow(stringResource(R.string.properties_modified), DateTimeFormatter.format(item.lastModifiedMs))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_confirm)) }
            }
        }
    }
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMicro)) {
        Text(label, color = AppColors.textMuted, fontSize = 11.sp)
        Text(value, color = AppColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
