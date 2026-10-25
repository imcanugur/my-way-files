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
import kotlinx.coroutines.flow.collect
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.TrashedItem
import tr.canugur.mywayfiles.ui.common.RetryErrorState
import tr.canugur.mywayfiles.ui.common.OperationErrorBanner
import tr.canugur.mywayfiles.ui.common.showOperationFeedback
import tr.canugur.mywayfiles.ui.common.AppModalBottomSheet
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions
import tr.canugur.mywayfiles.ui.trash.components.EmptyTrashConfirmationSheet
import tr.canugur.mywayfiles.ui.trash.components.PermanentDeleteConfirmationSheet
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
    LaunchedEffect(viewModel) {
        viewModel.operationFeedback.collect(context::showOperationFeedback)
    }

    var showEmptyTrashConfirm by remember { mutableStateOf(false) }
    var itemToDeletePermanently by remember { mutableStateOf<TrashedItem?>(null) }
    var itemToPreview by remember { mutableStateOf<TrashedItem?>(null) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showSelectedDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadItems()
    }

    BackHandler {
        if (selectedIds.isNotEmpty()) selectedIds = emptySet() else onNavigateBack()
    }

    val successState = state as? RecycleBinUiState.Success
    val hasItems = successState?.items?.isNotEmpty() == true
    val selectedItems = successState?.items.orEmpty().filter { it.id in selectedIds }
    LaunchedEffect(successState?.items?.map { it.id }) {
        val availableIds = successState?.items.orEmpty().map { it.id }.toSet()
        selectedIds = selectedIds.intersect(availableIds)
    }

    Scaffold(
        containerColor = AppColors.canvasBackground,
        topBar = {
            RecycleBinTopBar(
                itemCount = successState?.items?.size ?: 0,
                totalBytes = successState?.totalBytes ?: 0L,
                hasItems = hasItems,
                selectedCount = selectedItems.size,
                isAllSelected = successState?.items?.let { items ->
                    items.isNotEmpty() && selectedItems.size == items.size
                } == true,
                onNavigateBack = { if (selectedIds.isNotEmpty()) selectedIds = emptySet() else onNavigateBack() },
                onEmptyTrashClick = { showEmptyTrashConfirm = true },
                onToggleSelectAll = {
                    val allItems = successState?.items.orEmpty()
                    selectedIds = if (selectedItems.size == allItems.size) emptySet() else allItems.map { it.id }.toSet()
                },
                onRestoreSelected = {
                    viewModel.onEvent(RecycleBinUiEvent.RestoreSelected(selectedItems))
                    selectedIds = emptySet()
                },
                onDeleteSelected = { showSelectedDeleteConfirm = true }
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
                            isSelectionMode = selectedIds.isNotEmpty(),
                            isSelected = item.id in selectedIds,
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id
                                } else itemToPreview = item
                            },
                            onLongClick = { selectedIds = selectedIds + item.id },
                            onToggleSelection = {
                                selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id
                            },
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
            EmptyTrashConfirmationSheet(
                onConfirm = {
                    viewModel.onEvent(RecycleBinUiEvent.EmptyTrash)
                    showEmptyTrashConfirm = false
                },
                onDismiss = { showEmptyTrashConfirm = false }
            )
        }

        if (showSelectedDeleteConfirm) {
            AppModalBottomSheet(onDismissRequest = { showSelectedDeleteConfirm = false }) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.SpacingLarge)
                        .padding(bottom = Dimensions.SpacingLarge),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
                ) {
                    Text(
                        stringResource(R.string.action_delete_permanently),
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                    )
                    Text(stringResource(R.string.dialog_batch_permanent_delete_confirm, selectedItems.size))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        androidx.compose.material3.TextButton(onClick = { showSelectedDeleteConfirm = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        androidx.compose.material3.TextButton(onClick = {
                            viewModel.onEvent(RecycleBinUiEvent.DeleteSelectedPermanently(selectedItems))
                            selectedIds = emptySet()
                            showSelectedDeleteConfirm = false
                        }) {
                            Text(stringResource(R.string.action_delete_permanently), color = AppColors.statusCritical)
                        }
                    }
                }
            }
        }

        val permItem = itemToDeletePermanently
        if (permItem != null) {
            PermanentDeleteConfirmationSheet(
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
