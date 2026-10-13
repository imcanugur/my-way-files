package tr.canugur.mywayfiles.ui.explorer

import tr.canugur.mywayfiles.domain.model.FileItem

data class Breadcrumb(
    val title: String,
    val path: String
)

sealed interface ExplorerUiState {
    data object Loading : ExplorerUiState

    data object PermissionRequired : ExplorerUiState

    data class Success(
        val currentPath: String,
        val breadcrumbs: List<Breadcrumb>,
        val items: List<FileItem>,
        val isRefreshing: Boolean = false
    ) : ExplorerUiState

    data class Empty(
        val currentPath: String,
        val breadcrumbs: List<Breadcrumb>
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
}
