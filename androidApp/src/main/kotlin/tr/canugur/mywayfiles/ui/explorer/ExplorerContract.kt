package tr.canugur.mywayfiles.ui.explorer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import tr.canugur.mywayfiles.domain.model.ConflictResolution
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.domain.model.SortOption

enum class FileOperation {
    COPY, CUT, PASTE, RENAME, MOVE_TO_TRASH, DELETE, CREATE_FOLDER, MOVE_MEDIA,
    RESTORE, PERMANENT_DELETE, EMPTY_TRASH, ADD_FAVORITE, REMOVE_FAVORITE
}

data class MoveProgress(
    val itemIndex: Int,
    val itemCount: Int,
    val copiedBytes: Long,
    val totalBytes: Long
)

data class FileOperationProgress(
    val operation: FileOperation,
    val itemName: String,
    val itemIndex: Int,
    val itemCount: Int,
    val copiedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val canCancel: Boolean = false,
    val operationId: String = ""
)

data class OperationFeedback(
    val operation: FileOperation,
    val succeeded: Int,
    val failed: Int,
    val detail: String? = null,
    val cancelled: Boolean = false,
    val notProcessed: Int = 0
)

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

/** Shared across Explorer ViewModels so clipboard operations survive switching storage volumes. */
class FileClipboardStore {
    private val _clipboard = MutableStateFlow<Clipboard?>(null)
    val clipboard = _clipboard.asStateFlow()

    fun set(clipboard: Clipboard?) {
        _clipboard.value = clipboard
    }
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
    data class PasteWithResolution(val conflictResolution: ConflictResolution) : ExplorerUiEvent
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
