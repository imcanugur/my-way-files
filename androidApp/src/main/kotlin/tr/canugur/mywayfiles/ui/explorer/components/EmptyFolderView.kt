package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * Placeholder displayed when the active directory contains zero items.
 */
@Composable
fun EmptyFolderView(
    modifier: Modifier = Modifier,
    onCreateFolder: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimensions.SpacingHuge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_folder),
            contentDescription = null,
            tint = AppColors.textMuted,
            modifier = Modifier.size(Dimensions.IconSizeLarge)
        )

        Text(
            text = stringResource(R.string.folder_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textSecondary
        )
        if (onCreateFolder != null) {
            Button(onClick = onCreateFolder) {
                Text(stringResource(R.string.action_new_folder))
            }
        }
    }
}
