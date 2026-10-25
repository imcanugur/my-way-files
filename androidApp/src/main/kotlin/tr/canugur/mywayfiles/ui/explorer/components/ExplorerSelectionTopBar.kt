package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.ui.theme.AppColors

/** Minimal selection header; high-frequency file actions live in the thumb-reachable bottom bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerSelectionTopBar(
    selectedCount: Int,
    isAllSelected: Boolean,
    onCloseSelection: () -> Unit,
    onToggleSelectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onCloseSelection) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.action_cancel),
                    tint = AppColors.textPrimary
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.selected_count, selectedCount),
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.textPrimary,
                maxLines = 1
            )
        },
        actions = {
            TextButton(onClick = onToggleSelectAll) {
                Text(
                    text = stringResource(if (isAllSelected) R.string.deselect_all else R.string.select_all),
                    color = AppColors.brandAccent,
                    maxLines = 1
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.canvasBackground)
    )
}
