package tr.canugur.mywayfiles.ui.trash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.TrashedItem
import tr.canugur.mywayfiles.ui.common.RetryErrorState
import tr.canugur.mywayfiles.ui.common.OperationErrorBanner
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
                    androidx.compose.material3.Surface(
                        shape = CircleShape,
                        color = AppColors.brandAccentSubtle,
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_recycle_bin),
                                contentDescription = null,
                                tint = AppColors.brandAccent,
                                modifier = Modifier.size(Dimensions.IconSizeLarge)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Dimensions.SpacingLarge))
                    Text(
                        text = stringResource(R.string.recycle_bin_empty),
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = AppColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
                    Text(
                        text = stringResource(R.string.recycle_bin_description),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = AppColors.textSecondary
                    )
                    s.operationError?.let { error ->
                        Spacer(modifier = Modifier.height(Dimensions.SpacingDefault))
                        OperationErrorBanner(
                            message = error,
                            onDismiss = { viewModel.onEvent(RecycleBinUiEvent.ClearOperationError) }
                        )
                    }
                }
            }

            is RecycleBinUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    RetryErrorState(
                        message = s.message,
                        onRetry = { viewModel.onEvent(RecycleBinUiEvent.Refresh) }
                    )
                }
            }

            is RecycleBinUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        horizontal = Dimensions.SpacingLarge,
                        vertical = Dimensions.SpacingSmall
                    ),
                ) {
                    s.operationError?.let { error ->
                        item(key = "recycle-bin-operation-error") {
                            OperationErrorBanner(
                                message = error,
                                onDismiss = { viewModel.onEvent(RecycleBinUiEvent.ClearOperationError) },
                                modifier = Modifier.padding(bottom = Dimensions.SpacingSmall)
                            )
                        }
                    }
                    item(key = "recycle-bin-description") {
                        Text(
                            text = stringResource(R.string.recycle_bin_description),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = AppColors.textSecondary,
                            modifier = Modifier.padding(vertical = Dimensions.SpacingDefault)
                        )
                    }
                    itemsIndexed(s.items, key = { _, item -> item.id }) { index, item ->
                        TrashedItemRow(
                            item = item,
                            onClick = { itemToPreview = item },
                            onRestore = { viewModel.onEvent(RecycleBinUiEvent.Restore(item)) },
                            onDeletePermanently = { itemToDeletePermanently = item }
                        )
                        if (index < s.items.lastIndex) {
                            androidx.compose.material3.HorizontalDivider(color = AppColors.divider)
                        }
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
