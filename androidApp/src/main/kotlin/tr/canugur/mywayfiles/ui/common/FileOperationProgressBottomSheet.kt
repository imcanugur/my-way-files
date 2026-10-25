package tr.canugur.mywayfiles.ui.common

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.ui.explorer.FileOperation
import tr.canugur.mywayfiles.ui.explorer.FileOperationProgress
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * BottomSheet presented during background file operations (copy, move, delete, etc.).
 * Replaces the awkward top-floating banner with a standard, design-system compliant modal bottom sheet.
 */
@Composable
fun FileOperationProgressBottomSheet(
    progress: FileOperationProgress,
    onCancel: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val operationLabel = stringResource(
        when (progress.operation) {
            FileOperation.PASTE -> R.string.operation_paste
            FileOperation.DELETE, FileOperation.PERMANENT_DELETE -> R.string.operation_delete
            FileOperation.MOVE_TO_TRASH -> R.string.operation_move_to_trash
            FileOperation.MOVE_MEDIA -> R.string.operation_move
            FileOperation.RESTORE -> R.string.operation_restore
            FileOperation.EMPTY_TRASH -> R.string.operation_empty_trash
            else -> R.string.operation_paste
        }
    )
    val operationIconRes = when (progress.operation) {
        FileOperation.PASTE -> R.drawable.ic_paste
        FileOperation.DELETE, FileOperation.PERMANENT_DELETE, FileOperation.MOVE_TO_TRASH -> R.drawable.ic_delete
        FileOperation.MOVE_MEDIA -> R.drawable.ic_cut
        FileOperation.RESTORE -> R.drawable.ic_restore
        else -> R.drawable.ic_paste
    }
    val fraction = progress.takeIf { it.totalBytes > 0L }
        ?.let { (it.copiedBytes.toFloat() / it.totalBytes.toFloat()).coerceIn(0f, 1f) }

    AppModalBottomSheet(
        onDismissRequest = {
            if (progress.canCancel && onCancel != null) {
                onCancel()
            }
        },
        expandFullyOnShow = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Dimensions.SpacingLarge)
                .padding(bottom = Dimensions.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AppColors.brandAccent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(operationIconRes),
                        contentDescription = null,
                        tint = AppColors.brandAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.operation_running, operationLabel),
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (progress.itemName.isNotBlank()) {
                        Text(
                            text = stringResource(
                                R.string.operation_progress_item,
                                operationLabel,
                                progress.itemIndex + 1,
                                progress.itemCount,
                                progress.itemName
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (fraction != null) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AppColors.brandAccent,
                    trackColor = AppColors.divider
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(
                            R.string.move_progress_bytes,
                            Formatter.formatFileSize(context, progress.copiedBytes),
                            Formatter.formatFileSize(context, progress.totalBytes),
                            (fraction * 100f).toInt()
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.textSecondary
                    )
                    Text(
                        text = "${(fraction * 100f).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.brandAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AppColors.brandAccent,
                    trackColor = AppColors.divider
                )
            }

            if (progress.canCancel && onCancel != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text(
                            text = stringResource(R.string.operation_cancel),
                            color = AppColors.statusCritical,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
