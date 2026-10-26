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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

/** Dismissal hides this sheet only; operations keep running until explicitly cancelled or complete. */
@Composable
fun FileOperationProgressBottomSheet(
    progresses: List<FileOperationProgress>,
    onCancel: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (progresses.isEmpty()) return
    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        expandFullyOnShow = progresses.size > 1
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimensions.SpacingLarge)
                .padding(bottom = Dimensions.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingLarge)
        ) {
            progresses.forEach { progress ->
                OperationProgressContent(progress, onCancel = { onCancel(progress.operationId) })
            }
        }
    }
}

/** Compatibility entry point for screens that still expose one operation at a time. */
@Composable
fun FileOperationProgressBottomSheet(
    progress: FileOperationProgress,
    onCancel: (() -> Unit)? = null
) {
    var visible by remember(progress.operationId) { mutableStateOf(true) }
    if (visible) {
        FileOperationProgressBottomSheet(
            progresses = listOf(progress),
            onCancel = { onCancel?.invoke() },
            onDismiss = { visible = false }
        )
    }
}

@Composable
private fun OperationProgressContent(
    progress: FileOperationProgress,
    onCancel: () -> Unit
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

    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)) {
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
        if (fraction == null) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = AppColors.brandAccent,
                trackColor = AppColors.divider
            )
        } else {
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = AppColors.brandAccent,
                trackColor = AppColors.divider
            )
        }
        if (fraction != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
        }
        if (progress.canCancel) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.operation_cancel), color = AppColors.statusCritical)
                }
            }
        }
    }
}


@Composable
fun FileOperationDock(
    operationCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (operationCount <= 0) return
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Dimensions.CornerRadiusPill),
        color = AppColors.cardSurface,
        contentColor = AppColors.textPrimary,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.cardStroke),
        modifier = modifier.navigationBarsPadding().padding(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = AppColors.brandAccent,
                strokeWidth = 2.dp
            )
            Text(
                text = stringResource(R.string.background_operations, operationCount),
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
