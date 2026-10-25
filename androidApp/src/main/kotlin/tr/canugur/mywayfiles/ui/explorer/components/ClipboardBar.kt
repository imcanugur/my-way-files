package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import tr.canugur.mywayfiles.ui.explorer.Clipboard
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/** Floating, destination-aware confirmation for a pending cut or copy operation. */
@Composable
fun ClipboardBar(
    clipboard: Clipboard,
    destinationPath: String,
    destinationName: String,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isInvalidDestination = clipboard.items.any { item ->
        val source = File(item.path).absolutePath.trimEnd(File.separatorChar)
        val destination = File(destinationPath).absolutePath.trimEnd(File.separatorChar)
        val sourceParent = File(source).parentFile?.absolutePath?.trimEnd(File.separatorChar)
        sourceParent == destination || (item.isDirectory &&
            (destination == source || destination.startsWith(source + File.separator)))
    }
    val shape = RoundedCornerShape(Dimensions.CornerRadiusLarge)

    Surface(
        color = AppColors.cardSurface,
        contentColor = AppColors.textPrimary,
        shape = shape,
        shadowElevation = 4.dp,
        border = BorderStroke(Dimensions.CardBorderWidth, AppColors.cardStroke),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Dimensions.SpacingLarge, vertical = Dimensions.SpacingSmall)
            .clip(shape)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Dimensions.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(if (clipboard.isCut) R.drawable.ic_cut else R.drawable.ic_copy),
                    contentDescription = null,
                    tint = AppColors.brandAccent,
                    modifier = Modifier.size(Dimensions.IconSizeMedium)
                )
                Spacer(Modifier.width(Dimensions.SpacingMedium))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(if (clipboard.isCut) R.string.clipboard_move_to else R.string.clipboard_copy_to),
                        style = MaterialTheme.typography.titleSmall,
                        color = AppColors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                IconButton(onClick = onClear, modifier = Modifier.size(Dimensions.ActionButtonSize)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.action_cancel),
                        tint = AppColors.textSecondary
                    )
                }
            }
            Text(
                text = stringResource(R.string.clipboard_destination, destinationName),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isInvalidDestination) {
                Text(
                    text = stringResource(R.string.clipboard_choose_different_folder),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.statusWarning
                )
            }
            Button(
                onClick = onPaste,
                enabled = !isInvalidDestination,
                modifier = Modifier.fillMaxWidth().heightIn(min = Dimensions.ActionButtonSize),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.brandAccent,
                    contentColor = AppColors.cardSurface
                ),
                shape = RoundedCornerShape(Dimensions.CornerRadiusPill)
            ) {
                Text(
                    text = stringResource(if (clipboard.isCut) R.string.action_move_here else R.string.action_copy_here),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
