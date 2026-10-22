package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.core.util.DateTimeFormatter
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

@Composable
fun RenameDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.cardSurface,
        title = {
            Text(
                text = stringResource(R.string.dialog_rename_title),
                color = AppColors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
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
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank() && text != initialName) {
                        onConfirm(text.trim())
                    }
                    onDismiss()
                }
            ) {
                Text(text = stringResource(R.string.action_confirm), color = AppColors.brandAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel), color = AppColors.textSecondary)
            }
        }
    )
}

@Composable
fun CreateFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.cardSurface,
        title = {
            Text(
                text = stringResource(R.string.dialog_new_folder_title),
                color = AppColors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text(text = stringResource(R.string.dialog_new_folder_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.brandAccent,
                    unfocusedBorderColor = AppColors.cardStroke,
                    focusedTextColor = AppColors.textPrimary,
                    unfocusedTextColor = AppColors.textPrimary
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text.trim())
                    }
                    onDismiss()
                }
            ) {
                Text(text = stringResource(R.string.action_confirm), color = AppColors.brandAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel), color = AppColors.textSecondary)
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    fileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.cardSurface,
        title = {
            Text(
                text = stringResource(R.string.dialog_move_to_trash_title),
                color = AppColors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_move_to_trash_confirm, fileName),
                color = AppColors.textPrimary,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.statusCritical,
                    contentColor = AppColors.textPrimary
                ),
                shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
            ) {
                Text(
                    text = stringResource(R.string.action_move_to_trash),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = AppColors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
    )
}

@Composable
fun PropertiesDialog(
    item: FileItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.cardSurface,
        title = {
            Text(
                text = item.name,
                color = AppColors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)) {
                PropertyRow(
                    label = stringResource(R.string.properties_path),
                    value = item.path
                )
                PropertyRow(
                    label = stringResource(R.string.properties_size),
                    value = if (item.isDirectory) {
                        val count = item.childCount ?: 0
                        stringResource(R.string.items_count, count)
                    } else {
                        ByteFormatter.format(item.sizeBytes)
                    }
                )
                PropertyRow(
                    label = stringResource(R.string.properties_modified),
                    value = DateTimeFormatter.format(item.lastModifiedMs)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_confirm), color = AppColors.brandAccent)
            }
        }
    )
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Column {
        Text(text = label, color = AppColors.textMuted, fontSize = 11.sp)
        Text(text = value, color = AppColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
