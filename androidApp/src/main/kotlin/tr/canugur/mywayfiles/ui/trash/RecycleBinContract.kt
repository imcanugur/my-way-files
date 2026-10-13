package tr.canugur.mywayfiles.ui.trash

import tr.canugur.mywayfiles.domain.model.TrashedItem

sealed interface RecycleBinUiState {
    data object Loading : RecycleBinUiState
    data object Empty : RecycleBinUiState
    data class Success(
        val items: List<TrashedItem>,
        val totalBytes: Long
    ) : RecycleBinUiState
    data class Error(val message: String) : RecycleBinUiState
}

sealed interface RecycleBinUiEvent {
    data class Restore(val item: TrashedItem) : RecycleBinUiEvent
    data class DeletePermanently(val item: TrashedItem) : RecycleBinUiEvent
    data object EmptyTrash : RecycleBinUiEvent
    data object Refresh : RecycleBinUiEvent
}
