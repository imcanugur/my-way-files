package tr.canugur.mywayfiles.ui.trash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.TrashedItem
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions
import tr.canugur.mywayfiles.ui.trash.components.EmptyTrashConfirmDialog
import tr.canugur.mywayfiles.ui.trash.components.PermanentDeleteConfirmDialog
import tr.canugur.mywayfiles.ui.trash.components.RecycleBinTopBar
import tr.canugur.mywayfiles.ui.trash.components.TrashedItemPreviewDialog
import tr.canugur.mywayfiles.ui.trash.components.TrashedItemRow

/**
 * Enterprise Recycle Bin Screen orchestrating state, items list, and confirmation flows.
 */
@Composable
fun RecycleBinScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: RecycleBinViewModel = viewModel(
        factory = RecycleBinViewModel.provideFactory(context)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showEmptyTrashConfirm by remember { mutableStateOf(false) }
    var itemToDeletePermanently by remember { mutableStateOf<TrashedItem?>(null) }
    var itemToPreview by remember { mutableStateOf<TrashedItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadItems()
    }

    BackHandler {
        onNavigateBack()
    }

    val successState = state as? RecycleBinUiState.Success
    val hasItems = successState?.items?.isNotEmpty() == true

    Scaffold(
        containerColor = AppColors.canvasBackground,
        topBar = {
            RecycleBinTopBar(
                itemCount = successState?.items?.size ?: 0,
                totalBytes = successState?.totalBytes ?: 0L,
                hasItems = hasItems,
                onNavigateBack = onNavigateBack,
                onEmptyTrashClick = { showEmptyTrashConfirm = true }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        when (val s = state) {
            is RecycleBinUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AppColors.brandAccent,
                        strokeWidth = Dimensions.SpacingMicro
                    )
                }
            }

            is RecycleBinUiState.Empty -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(Dimensions.SpacingHuge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_recycle_bin),
                        contentDescription = null,
                        tint = AppColors.textMuted,
                        modifier = Modifier.size(Dimensions.IconSizeLarge)
                    )
                    Spacer(modifier = Modifier.height(Dimensions.SpacingMedium))
                    Text(
                        text = stringResource(R.string.recycle_bin_empty),
                        fontSize = 14.sp,
                        color = AppColors.textMuted
                    )
                }
            }

            is RecycleBinUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = s.message,
                        color = AppColors.statusCritical,
                        fontSize = 13.sp
                    )
                }
            }

            is RecycleBinUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        horizontal = Dimensions.SpacingDefault,
                        vertical = Dimensions.SpacingMedium
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                ) {
                    items(s.items, key = { it.id }) { item ->
                        TrashedItemRow(
                            item = item,
                            onClick = { itemToPreview = item },
                            onRestore = { viewModel.onEvent(RecycleBinUiEvent.Restore(item)) },
                            onDeletePermanently = { itemToDeletePermanently = item }
                        )
                    }
                }
            }
        }

        // Confirmation & Inspection Dialogs
        if (showEmptyTrashConfirm) {
            EmptyTrashConfirmDialog(
                onConfirm = {
                    viewModel.onEvent(RecycleBinUiEvent.EmptyTrash)
                    showEmptyTrashConfirm = false
                },
                onDismiss = { showEmptyTrashConfirm = false }
            )
        }

        val permItem = itemToDeletePermanently
        if (permItem != null) {
            PermanentDeleteConfirmDialog(
                item = permItem,
                onConfirm = {
                    viewModel.onEvent(RecycleBinUiEvent.DeletePermanently(permItem))
                    itemToDeletePermanently = null
                },
                onDismiss = { itemToDeletePermanently = null }
            )
        }

        val previewItem = itemToPreview
        if (previewItem != null) {
            TrashedItemPreviewDialog(
                item = previewItem,
                onRestore = {
                    viewModel.onEvent(RecycleBinUiEvent.Restore(previewItem))
                    itemToPreview = null
                },
                onDeletePermanently = {
                    itemToDeletePermanently = previewItem
                    itemToPreview = null
                },
                onDismiss = { itemToPreview = null }
            )
        }
    }
}
