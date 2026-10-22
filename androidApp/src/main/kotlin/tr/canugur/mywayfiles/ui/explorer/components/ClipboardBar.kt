package tr.canugur.mywayfiles.ui.explorer.components

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
import androidx.compose.material3.HorizontalDivider
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

/** Destination-aware action surface for the active copy or cut operation. */
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
        val sameSourceFolder = sourceParent == destination
        val destinationInsideSourceFolder = item.isDirectory &&
            (destination == source || destination.startsWith(source + File.separator))
        sameSourceFolder || destinationInsideSourceFolder
    }
    val shape = RoundedCornerShape(
        topStart = Dimensions.CornerRadiusLarge,
        topEnd = Dimensions.CornerRadiusLarge
    )

    Surface(
        color = AppColors.cardSurface,
        shape = shape,
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .clip(shape)
    ) {
        Column {
            HorizontalDivider(color = AppColors.divider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Dimensions.SpacingLarge, end = Dimensions.SpacingSmall, top = Dimensions.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(if (clipboard.isCut) R.drawable.ic_cut else R.drawable.ic_copy),
                    contentDescription = null,
                    tint = AppColors.brandAccent,
                    modifier = Modifier.size(Dimensions.IconSizeMedium)
                )
                Spacer(Modifier.width(Dimensions.SpacingMedium))
                Text(
                    text = stringResource(
                        if (clipboard.isCut) R.string.clipboard_move_to else R.string.clipboard_copy_to
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onClear, modifier = Modifier.size(Dimensions.ActionButtonSize)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.action_cancel),
                        tint = AppColors.textSecondary
                    )
                }
            }

            Text(
                text = if (clipboard.items.size == 1) clipboard.items.first().name
                else stringResource(R.string.items_count, clipboard.items.size),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = Dimensions.SpacingLarge)
            )
            Text(
                text = stringResource(R.string.clipboard_destination, destinationName),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(
                    horizontal = Dimensions.SpacingLarge,
                    vertical = Dimensions.SpacingExtraSmall
                )
            )

            if (isInvalidDestination) {
                Text(
                    text = stringResource(R.string.clipboard_choose_different_folder),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.statusWarning,
                    modifier = Modifier.padding(horizontal = Dimensions.SpacingLarge)
                )
            }

            Button(
                onClick = onPaste,
                enabled = !isInvalidDestination,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Dimensions.SpacingLarge,
                        vertical = Dimensions.SpacingSmall
                    )
                    .heightIn(min = Dimensions.ActionButtonSize),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.brandAccent,
                    contentColor = AppColors.cardSurface
                ),
                shape = RoundedCornerShape(Dimensions.CornerRadiusPill)
            ) {
                Text(
                    text = stringResource(
                        if (clipboard.isCut) R.string.action_move_here else R.string.action_copy_here
                    ),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
