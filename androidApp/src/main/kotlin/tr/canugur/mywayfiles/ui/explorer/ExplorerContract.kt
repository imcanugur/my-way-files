package tr.canugur.mywayfiles.ui.explorer

import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.domain.model.SortOption

data class Breadcrumb(
    val title: String,
    val path: String
)

data class Clipboard(
    val items: List<FileItem>,
    val isCut: Boolean
) {
    constructor(item: FileItem, isCut: Boolean) : this(listOf(item), isCut)
    val item: FileItem get() = items.first()
}

data class ExplorerSearchState(
    val results: List<FileItem> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null
)

sealed interface ExplorerUiState {
    data object Loading : ExplorerUiState

    data object PermissionRequired : ExplorerUiState

    data class Success(
        val currentPath: String,
        val breadcrumbs: List<Breadcrumb>,
        val items: List<FileItem>,
        val isRefreshing: Boolean = false,
        val clipboard: Clipboard? = null,
        val showHiddenFiles: Boolean = false,
        val sortOption: SortOption = SortOption.NAME_ASC,
        val operationError: String? = null
    ) : ExplorerUiState

    data class Empty(
        val currentPath: String,
        val breadcrumbs: List<Breadcrumb>,
        val clipboard: Clipboard? = null,
        val showHiddenFiles: Boolean = false,
        val sortOption: SortOption = SortOption.NAME_ASC,
        val operationError: String? = null
    ) : ExplorerUiState

    data class Error(
        val message: String,
        val currentPath: String,
        val breadcrumbs: List<Breadcrumb>
    ) : ExplorerUiState
}

sealed interface ExplorerUiEvent {
    data class ItemClick(val item: FileItem) : ExplorerUiEvent
    data class NavigateToBreadcrumb(val path: String) : ExplorerUiEvent
    data object NavigateUp : ExplorerUiEvent
    data object Refresh : ExplorerUiEvent
    data object CheckPermission : ExplorerUiEvent
    data class Copy(val item: FileItem) : ExplorerUiEvent
    data class Cut(val item: FileItem) : ExplorerUiEvent
    data class BatchCopy(val items: List<FileItem>) : ExplorerUiEvent
    data class BatchCut(val items: List<FileItem>) : ExplorerUiEvent
    data object Paste : ExplorerUiEvent
    data object ClearClipboard : ExplorerUiEvent
    data object ClearOperationError : ExplorerUiEvent
    data class Rename(val item: FileItem, val newName: String) : ExplorerUiEvent
    data class MoveToTrash(val item: FileItem) : ExplorerUiEvent
    data class Delete(val item: FileItem) : ExplorerUiEvent
    data class BatchMoveToTrash(val items: List<FileItem>) : ExplorerUiEvent
    data class BatchDelete(val items: List<FileItem>) : ExplorerUiEvent
    data class CreateFolder(val folderName: String) : ExplorerUiEvent
    data object ToggleShowHiddenFiles : ExplorerUiEvent
    data class ChangeSortOption(val sortOption: SortOption) : ExplorerUiEvent
}
