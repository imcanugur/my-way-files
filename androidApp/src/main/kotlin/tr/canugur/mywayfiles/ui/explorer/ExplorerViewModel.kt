package tr.canugur.mywayfiles.ui.explorer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tr.canugur.mywayfiles.data.datasource.AndroidFileLocalDataSource
import tr.canugur.mywayfiles.data.repository.FileRepositoryImpl
import tr.canugur.mywayfiles.data.repository.RecycleBinRepositoryImpl
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.domain.model.SortOption
import tr.canugur.mywayfiles.domain.repository.FileRepository
import tr.canugur.mywayfiles.domain.repository.RecycleBinRepository
import tr.canugur.mywayfiles.domain.usecase.GetDirectoryContentsUseCase
import java.io.File

/**
 * Production-ready ViewModel governing file system browsing, breadcrumb navigation,
 * universal file operations (copy, cut, paste, rename, delete, mkdir), and Recycle Bin integration.
 */
class ExplorerViewModel(
    val rootPath: String,
    private val driveName: String,
    private val getDirectoryContentsUseCase: GetDirectoryContentsUseCase,
    private val fileRepository: FileRepository,
    private val recycleBinRepository: RecycleBinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExplorerUiState>(ExplorerUiState.Loading)
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    private var activePath: String = rootPath
    private var activeClipboard: Clipboard? = null
    private var showHiddenFiles: Boolean = false
    private var sortOption: SortOption = SortOption.NAME_ASC

    init {
        loadDirectory(rootPath)
    }

    fun onEvent(event: ExplorerUiEvent) {
        when (event) {
            is ExplorerUiEvent.ItemClick -> handleItemClick(event.item)
            is ExplorerUiEvent.NavigateToBreadcrumb -> loadDirectory(event.path)
            is ExplorerUiEvent.NavigateUp -> navigateUp()
            is ExplorerUiEvent.Refresh -> loadDirectory(activePath, isRefreshing = true)
            is ExplorerUiEvent.CheckPermission -> {
                if (fileRepository.hasStoragePermission()) {
                    loadDirectory(activePath)
                } else {
                    _uiState.value = ExplorerUiState.PermissionRequired
                }
            }
            is ExplorerUiEvent.Copy -> {
                activeClipboard = Clipboard(item = event.item, isCut = false)
                updateStateClipboard()
            }
            is ExplorerUiEvent.Cut -> {
                activeClipboard = Clipboard(item = event.item, isCut = true)
                updateStateClipboard()
            }
            is ExplorerUiEvent.BatchCopy -> {
                if (event.items.isNotEmpty()) {
                    activeClipboard = Clipboard(items = event.items, isCut = false)
                    updateStateClipboard()
                }
            }
            is ExplorerUiEvent.BatchCut -> {
                if (event.items.isNotEmpty()) {
                    activeClipboard = Clipboard(items = event.items, isCut = true)
                    updateStateClipboard()
                }
            }
            is ExplorerUiEvent.ClearClipboard -> {
                activeClipboard = null
                updateStateClipboard()
            }
            is ExplorerUiEvent.Paste -> handlePaste()
            is ExplorerUiEvent.Rename -> handleRename(event.item, event.newName)
            is ExplorerUiEvent.MoveToTrash -> handleMoveToTrash(event.item)
            is ExplorerUiEvent.Delete -> handleDelete(event.item)
            is ExplorerUiEvent.BatchMoveToTrash -> handleBatchMoveToTrash(event.items)
            is ExplorerUiEvent.BatchDelete -> handleBatchDelete(event.items)
            is ExplorerUiEvent.CreateFolder -> handleCreateFolder(event.folderName)
            is ExplorerUiEvent.ToggleShowHiddenFiles -> {
                showHiddenFiles = !showHiddenFiles
                loadDirectory(activePath, isRefreshing = true)
            }
            is ExplorerUiEvent.ChangeSortOption -> {
                sortOption = event.sortOption
                loadDirectory(activePath, isRefreshing = true)
            }
        }
    }

    private fun handleItemClick(item: FileItem) {
        if (item.isDirectory) {
            loadDirectory(item.path)
        }
    }

    private fun handlePaste() {
        val clip = activeClipboard ?: return
        viewModelScope.launch {
            var allSucceeded = true
            for (item in clip.items) {
                val result = if (clip.isCut) {
                    fileRepository.move(item.path, activePath)
                } else {
                    fileRepository.copy(item.path, activePath)
                }
                if (result.isFailure) allSucceeded = false
            }
            if (allSucceeded && clip.isCut) {
                activeClipboard = null
            }
            loadDirectory(activePath, isRefreshing = true)
        }
    }

    private fun handleRename(item: FileItem, newName: String) {
        viewModelScope.launch {
            fileRepository.rename(item.path, newName)
            loadDirectory(activePath, isRefreshing = true)
        }
    }

    private fun handleMoveToTrash(item: FileItem) {
        viewModelScope.launch {
            recycleBinRepository.moveToTrash(item)
            loadDirectory(activePath, isRefreshing = true)
        }
    }

    private fun handleBatchMoveToTrash(items: List<FileItem>) {
        viewModelScope.launch {
            for (item in items) {
                recycleBinRepository.moveToTrash(item)
            }
            loadDirectory(activePath, isRefreshing = true)
        }
    }

    private fun handleDelete(item: FileItem) {
        viewModelScope.launch {
            fileRepository.delete(item.path)
            loadDirectory(activePath, isRefreshing = true)
        }
    }

    private fun handleBatchDelete(items: List<FileItem>) {
        viewModelScope.launch {
            for (item in items) {
                fileRepository.delete(item.path)
            }
            loadDirectory(activePath, isRefreshing = true)
        }
    }

    private fun handleCreateFolder(name: String) {
        viewModelScope.launch {
            fileRepository.createFolder(activePath, name)
            loadDirectory(activePath, isRefreshing = true)
        }
    }

    private fun updateStateClipboard() {
        when (val state = _uiState.value) {
            is ExplorerUiState.Success -> {
                _uiState.value = state.copy(clipboard = activeClipboard)
            }
            is ExplorerUiState.Empty -> {
                _uiState.value = state.copy(clipboard = activeClipboard)
            }
            else -> Unit
        }
    }

    fun canNavigateUp(): Boolean {
        val rootNormalized = File(rootPath).canonicalPath
        val activeNormalized = File(activePath).canonicalPath
        return activeNormalized != rootNormalized && activeNormalized.startsWith(rootNormalized)
    }

    fun navigateUp(): Boolean {
        if (!canNavigateUp()) return false
        val parent = File(activePath).parentFile ?: return false
        val rootCanonical = File(rootPath).canonicalPath
        if (parent.canonicalPath.startsWith(rootCanonical)) {
            loadDirectory(parent.canonicalPath)
            return true
        }
        return false
    }

    fun loadDirectory(path: String, isRefreshing: Boolean = false) {
        viewModelScope.launch {
            if (!fileRepository.hasStoragePermission()) {
                _uiState.value = ExplorerUiState.PermissionRequired
                return@launch
            }

            activePath = path
            val breadcrumbs = buildBreadcrumbs(path)

            if (!isRefreshing) {
                _uiState.value = ExplorerUiState.Loading
            }

            getDirectoryContentsUseCase(path, showHidden = showHiddenFiles, sortOption = sortOption).fold(
                onSuccess = { items ->
                    _uiState.value = if (items.isEmpty()) {
                        ExplorerUiState.Empty(
                            currentPath = path,
                            breadcrumbs = breadcrumbs,
                            clipboard = activeClipboard,
                            showHiddenFiles = showHiddenFiles,
                            sortOption = sortOption
                        )
                    } else {
                        ExplorerUiState.Success(
                            currentPath = path,
                            breadcrumbs = breadcrumbs,
                            items = items,
                            isRefreshing = false,
                            clipboard = activeClipboard,
                            showHiddenFiles = showHiddenFiles,
                            sortOption = sortOption
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = ExplorerUiState.Error(
                        message = error.localizedMessage ?: "Failed to read directory",
                        currentPath = path,
                        breadcrumbs = breadcrumbs
                    )
                }
            )
        }
    }

    private fun buildBreadcrumbs(currentPath: String): List<Breadcrumb> {
        val crumbs = mutableListOf<Breadcrumb>()
        val rootFile = File(rootPath)
        val currentFile = File(currentPath)

        crumbs.add(Breadcrumb(title = driveName, path = rootFile.canonicalPath))

        val rootCanonical = rootFile.canonicalPath
        val currentCanonical = currentFile.canonicalPath

        if (currentCanonical == rootCanonical) {
            return crumbs
        }

        if (currentCanonical.startsWith(rootCanonical)) {
            val relative = currentCanonical.removePrefix(rootCanonical).trimStart(File.separatorChar)
            if (relative.isNotEmpty()) {
                val segments = relative.split(File.separatorChar)
                var accumulated = rootCanonical
                for (segment in segments) {
                    accumulated += File.separator + segment
                    crumbs.add(Breadcrumb(title = segment, path = accumulated))
                }
            }
        }
        return crumbs
    }

    companion object {
        fun provideFactory(
            context: Context,
            rootPath: String,
            driveName: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val dataSource = AndroidFileLocalDataSource(appContext)
                val repository = FileRepositoryImpl(dataSource)
                val recycleBinRepository = RecycleBinRepositoryImpl.getInstance(appContext)
                val useCase = GetDirectoryContentsUseCase(repository)
                return ExplorerViewModel(
                    rootPath = rootPath,
                    driveName = driveName,
                    getDirectoryContentsUseCase = useCase,
                    fileRepository = repository,
                    recycleBinRepository = recycleBinRepository
                ) as T
            }
        }
    }
}
