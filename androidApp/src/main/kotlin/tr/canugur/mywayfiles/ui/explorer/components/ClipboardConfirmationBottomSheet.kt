package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.core.util.ByteFormatter
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.common.AppModalBottomSheet
import tr.canugur.mywayfiles.ui.explorer.Clipboard
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * Standard modal bottom sheet for confirming a pending paste or move operation.
 * Conforms 100% with the App Factory design system and provides full visibility
 * of source items, target destination, and invalid state warnings.
 */
@Composable
fun ClipboardConfirmationBottomSheet(
    clipboard: Clipboard,
    destinationPath: String,
    destinationName: String,
    isOperating: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isInvalidDestination = clipboard.items.any { item ->
        val source = File(item.path).absolutePath.trimEnd(File.separatorChar)
        val destination = File(destinationPath).absolutePath.trimEnd(File.separatorChar)
        val sourceParent = File(source).parentFile?.absolutePath?.trimEnd(File.separatorChar)
        sourceParent == destination || (item.isDirectory &&
            (destination == source || destination.startsWith(source + File.separator)))
    }

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
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
            // Header
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
                        painter = painterResource(if (clipboard.isCut) R.drawable.ic_cut else R.drawable.ic_copy),
                        contentDescription = null,
                        tint = AppColors.brandAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(if (clipboard.isCut) R.string.clipboard_move_to else R.string.clipboard_copy_to),
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (clipboard.items.size == 1) clipboard.items.first().name
                        else stringResource(R.string.items_count, clipboard.items.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Target destination card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimensions.CornerRadiusMedium))
                    .background(AppColors.cardSurfaceSubtle)
                    .border(1.dp, AppColors.cardStroke, RoundedCornerShape(Dimensions.CornerRadiusMedium))
                    .padding(Dimensions.SpacingMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_folder),
                    contentDescription = null,
                    tint = AppColors.brandAccent,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = destinationName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimary
                    )
                    Text(
                        text = destinationPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Items Preview List (if multiple or single)
            val previewItems = clipboard.items.take(3)
            val remainingCount = clipboard.items.size - previewItems.size

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimensions.CornerRadiusMedium))
                    .background(AppColors.cardSurface)
                    .border(1.dp, AppColors.divider, RoundedCornerShape(Dimensions.CornerRadiusMedium))
                    .padding(horizontal = Dimensions.SpacingMedium, vertical = Dimensions.SpacingSmall),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMicro)
            ) {
                previewItems.forEach { item ->
                    ClipboardItemRow(item = item)
                }
                if (remainingCount > 0) {
                    Text(
                        text = "+ $remainingCount diğer öğe",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.textSecondary,
                        modifier = Modifier.padding(start = 28.dp, top = 2.dp, bottom = 4.dp)
                    )
                }
            }

            // Warning if destination is invalid
            if (isInvalidDestination) {
                Text(
                    text = stringResource(R.string.clipboard_choose_different_folder),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.statusWarning
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.SpacingMicro))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = Dimensions.ActionButtonSize)
                ) {
                    Text(stringResource(R.string.action_cancel), color = AppColors.textSecondary)
                }

                Spacer(modifier = Modifier.width(Dimensions.SpacingMedium))

                Button(
                    onClick = onConfirm,
                    enabled = !isInvalidDestination && !isOperating,
                    modifier = Modifier.heightIn(min = Dimensions.ActionButtonSize),
                    shape = RoundedCornerShape(Dimensions.CornerRadiusPill),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.brandAccent)
                ) {
                    Text(
                        text = stringResource(if (clipboard.isCut) R.string.action_move_here else R.string.action_copy_here),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun ClipboardItemRow(item: FileItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
    ) {
        Icon(
            painter = painterResource(if (item.isDirectory) R.drawable.ic_folder else R.drawable.ic_document),
            contentDescription = null,
            tint = if (item.isDirectory) AppColors.brandAccent else AppColors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (!item.isDirectory) {
            Text(
                text = ByteFormatter.format(item.sizeBytes),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.textSecondary
            )
        }
    }
}
