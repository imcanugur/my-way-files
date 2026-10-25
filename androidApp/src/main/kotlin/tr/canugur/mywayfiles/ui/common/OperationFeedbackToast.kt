package tr.canugur.mywayfiles.ui.common

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.ui.explorer.FileOperation
import tr.canugur.mywayfiles.ui.explorer.OperationFeedback

/** Shows localized feedback only after a file operation has returned its actual result. */
fun Context.showOperationFeedback(feedback: OperationFeedback) {
    val label = getString(
        when (feedback.operation) {
            FileOperation.COPY -> R.string.operation_copy
            FileOperation.CUT -> R.string.operation_cut
            FileOperation.PASTE -> R.string.operation_paste
            FileOperation.RENAME -> R.string.operation_rename
            FileOperation.MOVE_TO_TRASH -> R.string.operation_move_to_trash
            FileOperation.DELETE, FileOperation.PERMANENT_DELETE -> R.string.operation_delete
            FileOperation.CREATE_FOLDER -> R.string.operation_create_folder
            FileOperation.MOVE_MEDIA -> R.string.operation_move
            FileOperation.RESTORE -> R.string.operation_restore
            FileOperation.EMPTY_TRASH -> R.string.operation_empty_trash
            FileOperation.ADD_FAVORITE -> R.string.operation_add_favorite
            FileOperation.REMOVE_FAVORITE -> R.string.operation_remove_favorite
        }
    )
    val message = when {
        feedback.cancelled && feedback.failed > 0 -> getString(
            R.string.operation_cancelled_partial,
            label,
            feedback.succeeded,
            feedback.failed,
            feedback.notProcessed
        )
        feedback.cancelled -> getString(
            R.string.operation_cancelled_with_progress,
            label,
            feedback.succeeded,
            feedback.notProcessed
        )
        feedback.failed == 0 && feedback.succeeded > 1 ->
            getString(R.string.operation_completed_count, label, feedback.succeeded)
        feedback.failed == 0 -> getString(R.string.operation_completed, label)
        feedback.succeeded > 0 -> getString(
            R.string.operation_partial, label, feedback.succeeded, feedback.failed
        )
        else -> getString(R.string.operation_failed, label)
    }
    val accurateMessage = if (feedback.failed == 0 &&
        (feedback.operation == FileOperation.COPY || feedback.operation == FileOperation.CUT)
    ) {
        val verb = getString(
            if (feedback.operation == FileOperation.COPY) R.string.operation_copy_verb
            else R.string.operation_move_verb
        )
        if (feedback.succeeded == 1) {
            getString(R.string.operation_clipboard_ready_one, verb)
        } else {
            getString(R.string.operation_clipboard_ready_many, feedback.succeeded, verb)
        }
    } else {
        message
    }
    val withDetail = feedback.detail?.takeIf { feedback.failed > 0 }?.let { "$accurateMessage. $it" } ?: accurateMessage
    Toast.makeText(this, withDetail, if (feedback.failed > 0) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}


@androidx.compose.runtime.Composable
fun FileOperationProgressBanner(
    progress: tr.canugur.mywayfiles.ui.explorer.FileOperationProgress,
    onCancel: (() -> Unit)? = null,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val operationLabel = androidx.compose.ui.res.stringResource(
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
    val fraction = progress.takeIf { it.totalBytes > 0L }
        ?.let { (it.copiedBytes.toFloat() / it.totalBytes.toFloat()).coerceIn(0f, 1f) }
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = androidx.compose.material3.MaterialTheme.shapes.large,
        color = tr.canugur.mywayfiles.ui.theme.AppColors.cardSurface,
        shadowElevation = 2.dp
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.Text(
                    text = if (progress.itemName.isBlank()) {
                        androidx.compose.ui.res.stringResource(R.string.operation_running, operationLabel)
                    } else {
                        androidx.compose.ui.res.stringResource(
                            R.string.operation_progress_item,
                            operationLabel,
                            progress.itemIndex + 1,
                            progress.itemCount,
                            progress.itemName
                        )
                    },
                    modifier = androidx.compose.ui.Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
                if (progress.canCancel && onCancel != null) {
                    androidx.compose.material3.TextButton(onClick = onCancel) {
                        androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(R.string.operation_cancel))
                    }
                }
            }
            if (fraction != null) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = fraction,
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                )
                androidx.compose.material3.Text(
                    text = androidx.compose.ui.res.stringResource(
                        R.string.move_progress_bytes,
                        android.text.format.Formatter.formatFileSize(context, progress.copiedBytes),
                        android.text.format.Formatter.formatFileSize(context, progress.totalBytes),
                        (fraction * 100f).toInt()
                    ),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    modifier = androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.End)
                )
            } else {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                )
            }
        }
    }
}
