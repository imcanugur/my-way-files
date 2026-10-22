package tr.canugur.mywayfiles.ui.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tr.canugur.mywayfiles.ui.theme.AppColors

/** Shared modal surface for transient tasks across the app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = AppColors.cardSurface,
        contentColor = AppColors.textPrimary,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        content = content
    )
}
