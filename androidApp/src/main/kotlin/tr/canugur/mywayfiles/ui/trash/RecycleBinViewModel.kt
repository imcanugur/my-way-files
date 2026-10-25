package tr.canugur.mywayfiles.ui.trash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import tr.canugur.mywayfiles.data.repository.RecycleBinRepositoryImpl
import tr.canugur.mywayfiles.domain.model.TrashedItem
import tr.canugur.mywayfiles.ui.explorer.FileOperation
import tr.canugur.mywayfiles.ui.explorer.OperationFeedback
import tr.canugur.mywayfiles.domain.repository.RecycleBinRepository

/**
 * ViewModel managing Recycle Bin state flow, restore operations, and purging.
 */
class RecycleBinViewModel(
    private val repository: RecycleBinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecycleBinUiState>(RecycleBinUiState.Loading)
    val uiState: StateFlow<RecycleBinUiState> = _uiState.asStateFlow()
    private val _operationFeedback = MutableSharedFlow<OperationFeedback>(extraBufferCapacity = 8)
    val operationFeedback = _operationFeedback.asSharedFlow()
    private val _activeOperation = MutableStateFlow<tr.canugur.mywayfiles.ui.explorer.FileOperationProgress?>(null)
    val activeOperation: StateFlow<tr.canugur.mywayfiles.ui.explorer.FileOperationProgress?> = _activeOperation.asStateFlow()
    private var loadRequestId = 0L
    private var operationInProgress = false

    fun onEvent(event: RecycleBinUiEvent) {
        when (event) {
            is RecycleBinUiEvent.Refresh -> loadItems()
            is RecycleBinUiEvent.Restore -> restore(event.item)
            is RecycleBinUiEvent.RestoreSelected -> restoreSelected(event.items)
            is RecycleBinUiEvent.DeletePermanently -> deletePermanently(event.item)
            is RecycleBinUiEvent.DeleteSelectedPermanently -> deleteSelectedPermanently(event.items)
            is RecycleBinUiEvent.EmptyTrash -> emptyTrash()
            is RecycleBinUiEvent.ClearOperationError -> clearOperationError()
        }
    }

    fun loadItems(operationError: String? = null) {
        val requestId = ++loadRequestId
        viewModelScope.launch {
            if (_uiState.value !is RecycleBinUiState.Success && _uiState.value !is RecycleBinUiState.Empty) {
                _uiState.value = RecycleBinUiState.Loading
            }
            repository.getTrashedItems().fold(
                onSuccess = { items ->
                    if (requestId != loadRequestId) return@fold
                    if (items.isEmpty()) {
                        _uiState.value = RecycleBinUiState.Empty(operationError)
                    } else {
                        val total = items.sumOf { it.sizeBytes }
                        _uiState.value = RecycleBinUiState.Success(
                            items = items,
                            totalBytes = total,
                            operationError = operationError
                        )
                    }
                },
                onFailure = { error ->
                    if (requestId != loadRequestId) return@fold
                    val readError = error.localizedMessage ?: "Failed to read Recycle Bin"
                    _uiState.value = RecycleBinUiState.Error(
                        if (operationError == null) readError else "$operationError. $readError"
                    )
                }
            )
        }
    }

    private fun restore(item: TrashedItem) = runTracked(FileOperation.RESTORE, listOf(item)) { item, progress ->
        repository.restore(item, progress)
    }

    private fun deletePermanently(item: TrashedItem) =
        runTracked(FileOperation.PERMANENT_DELETE, listOf(item)) { selected, _ -> repository.deletePermanently(selected) }

    private fun restoreSelected(items: List<TrashedItem>) =
        runTracked(FileOperation.RESTORE, items) { item, progress -> repository.restore(item, progress) }

    private fun deleteSelectedPermanently(items: List<TrashedItem>) =
        runTracked(FileOperation.PERMANENT_DELETE, items) { selected, _ -> repository.deletePermanently(selected) }

    private fun emptyTrash() {
        if (operationInProgress) return
        operationInProgress = true
        _activeOperation.value = tr.canugur.mywayfiles.ui.explorer.FileOperationProgress(
            FileOperation.EMPTY_TRASH, "", 0, 1
        )
        viewModelScope.launch {
            try {
                val result = repository.emptyTrash()
                _operationFeedback.tryEmit(OperationFeedback(
                    FileOperation.EMPTY_TRASH, if (result.isSuccess) 1 else 0, if (result.isFailure) 1 else 0,
                    result.exceptionOrNull()?.localizedMessage
                ))
                result.fold(onSuccess = { loadItems() }, onFailure = { loadItems(it.localizedMessage ?: "Unable to empty Recycle Bin") })
            } finally {
                _activeOperation.value = null
                operationInProgress = false
            }
        }
    }

    private fun runTracked(
        operation: FileOperation,
        items: List<TrashedItem>,
        action: suspend (TrashedItem, (Long, Long) -> Unit) -> Result<Unit>
    ) {
        if (operationInProgress || items.isEmpty()) return
        operationInProgress = true
        viewModelScope.launch {
            val results = mutableListOf<Result<Unit>>()
            try {
                items.forEachIndexed { index, item ->
                    _activeOperation.value = tr.canugur.mywayfiles.ui.explorer.FileOperationProgress(
                        operation, item.name, index, items.size
                    )
                    results += try {
                        action(item) { copied, total ->
                            _activeOperation.value = tr.canugur.mywayfiles.ui.explorer.FileOperationProgress(
                                operation, item.name, index, items.size, copied, total
                            )
                        }
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Result.failure(error)
                    }
                }
                val failures = results.count { it.isFailure }
                val succeeded = results.size - failures
                _operationFeedback.tryEmit(OperationFeedback(operation, succeeded, failures,
                    results.firstNotNullOfOrNull { it.exceptionOrNull()?.localizedMessage }))
                val fallback = if (operation == FileOperation.RESTORE) "Unable to restore selected items"
                    else "Unable to permanently delete selected items"
                loadItems(if (failures == 0) null else results.firstNotNullOfOrNull { it.exceptionOrNull()?.localizedMessage } ?: fallback)
            } finally {
                _activeOperation.value = null
                operationInProgress = false
            }
        }
    }

    private fun clearOperationError() {
        when (val state = _uiState.value) {
            is RecycleBinUiState.Success -> _uiState.value = state.copy(operationError = null)
            is RecycleBinUiState.Empty -> _uiState.value = state.copy(operationError = null)
            else -> Unit
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val repo = RecycleBinRepositoryImpl.getInstance(context.applicationContext)
                    return RecycleBinViewModel(repo) as T
                }
            }
    }
}
