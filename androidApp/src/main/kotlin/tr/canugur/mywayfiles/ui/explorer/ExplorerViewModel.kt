package tr.canugur.mywayfiles.ui.explorer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import tr.canugur.mywayfiles.data.cache.DirectoryCache
import tr.canugur.mywayfiles.data.datasource.AndroidFileLocalDataSource
import tr.canugur.mywayfiles.data.preferences.ExplorerPreferences
import tr.canugur.mywayfiles.data.repository.FileRepositoryImpl
import tr.canugur.mywayfiles.data.repository.RecycleBinRepositoryImpl
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.domain.model.SortOption
import tr.canugur.mywayfiles.domain.repository.FileRepository
import tr.canugur.mywayfiles.domain.repository.RecycleBinRepository
import tr.canugur.mywayfiles.domain.usecase.GetDirectoryContentsUseCase
import tr.canugur.mywayfiles.domain.usecase.SearchFilesUseCase
import java.io.File
import java.util.Locale

/**
 * Production-ready ViewModel governing file system browsing, breadcrumb navigation,
 * universal file operations (copy, cut, paste, rename, delete, mkdir), and Recycle Bin integration.
 */
class ExplorerViewModel(
    val rootPath: String,
    initialPath: String = rootPath,
    private val driveName: String,
    private val getDirectoryContentsUseCase: GetDirectoryContentsUseCase,
    private val fileRepository: FileRepository,
    private val recycleBinRepository: RecycleBinRepository,
    private val explorerPreferences: ExplorerPreferences,
    private val searchFilesUseCase: SearchFilesUseCase,
    private val clipboardStore: FileClipboardStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExplorerUiState>(ExplorerUiState.Loading)
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    private val _searchState = MutableStateFlow(ExplorerSearchState())
    private val _operationFeedback = MutableSharedFlow<OperationFeedback>(extraBufferCapacity = 8)
    val operationFeedback = _operationFeedback.asSharedFlow()
    val searchState: StateFlow<ExplorerSearchState> = _searchState.asStateFlow()

    private var activePath: String = rootPath
    private var activeClipboard: Clipboard?
        get() = clipboardStore.clipboard.value
        set(value) = clipboardStore.set(value)
    private var showHiddenFiles: Boolean = explorerPreferences.showHiddenFiles
    private var sortOption: SortOption = explorerPreferences.sortOption
    private var directoryRequestId: Long = 0L
    private var searchRequestId: Long = 0L
    private var searchJob: Job? = null

    init {
        activePath = initialPath
        viewModelScope.launch {
            clipboardStore.clipboard.collect { updateStateClipboard() }
        }
        loadDirectory(initialPath)
    }

    fun onEvent(event: ExplorerUiEvent) {
        when (event) {
            is ExplorerUiEvent.ItemClick -> handleItemClick(event.item)
            is ExplorerUiEvent.NavigateToBreadcrumb -> loadDirectory(event.path)
            is ExplorerUiEvent.NavigateUp -> navigateUp()
            is ExplorerUiEvent.Refresh -> loadDirectory(activePath, isRefreshing = true)
            is ExplorerUiEvent.CheckPermission -> {
                if (fileRepository.hasStoragePermission()) {
                    showHiddenFiles = explorerPreferences.showHiddenFiles
                    loadDirectory(activePath)
                } else {
                    _uiState.value = ExplorerUiState.PermissionRequired
                }
            }
            is ExplorerUiEvent.Copy -> {
                activeClipboard = Clipboard(item = event.item, isCut = false)
                updateStateClipboard()
                _operationFeedback.tryEmit(OperationFeedback(FileOperation.COPY, 1, 0))
            }
            is ExplorerUiEvent.Cut -> {
                activeClipboard = Clipboard(item = event.item, isCut = true)
                updateStateClipboard()
                _operationFeedback.tryEmit(OperationFeedback(FileOperation.CUT, 1, 0))
            }
            is ExplorerUiEvent.BatchCopy -> {
                if (event.items.isNotEmpty()) {
                    activeClipboard = Clipboard(items = event.items, isCut = false)
                    updateStateClipboard()
                    _operationFeedback.tryEmit(OperationFeedback(FileOperation.COPY, event.items.size, 0))
                }
            }
            is ExplorerUiEvent.BatchCut -> {
                if (event.items.isNotEmpty()) {
                    activeClipboard = Clipboard(items = event.items, isCut = true)
                    updateStateClipboard()
                    _operationFeedback.tryEmit(OperationFeedback(FileOperation.CUT, event.items.size, 0))
                }
            }
            is ExplorerUiEvent.ClearClipboard -> {
                activeClipboard = null
                updateStateClipboard()
            }
            is ExplorerUiEvent.ClearOperationError -> clearOperationError()
            is ExplorerUiEvent.Paste -> handlePaste()
            is ExplorerUiEvent.Rename -> handleRename(event.item, event.newName)
            is ExplorerUiEvent.MoveToTrash -> handleMoveToTrash(event.item)
            is ExplorerUiEvent.Delete -> handleDelete(event.item)
            is ExplorerUiEvent.BatchMoveToTrash -> handleBatchMoveToTrash(event.items)
            is ExplorerUiEvent.BatchDelete -> handleBatchDelete(event.items)
            is ExplorerUiEvent.CreateFolder -> handleCreateFolder(event.folderName)
            is ExplorerUiEvent.ToggleShowHiddenFiles -> {
                showHiddenFiles = !showHiddenFiles
                explorerPreferences.showHiddenFiles = showHiddenFiles
                loadDirectory(activePath, isRefreshing = true)
            }
            is ExplorerUiEvent.ChangeSortOption -> {
                sortOption = event.sortOption
                explorerPreferences.sortOption = event.sortOption
                loadDirectory(activePath, isRefreshing = true)
            }
        }
    }

    fun search(query: String) {
        val requestId = ++searchRequestId
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchState.value = ExplorerSearchState()
            return
        }

        val path = activePath
        val includeHidden = showHiddenFiles
        val sort = sortOption
        _searchState.value = ExplorerSearchState(isSearching = true)
        searchJob = viewModelScope.launch {
            searchFilesUseCase(
                path = path,
                query = query,
                showHidden = includeHidden,
                sortOption = sort,
                onResults = { partialResults ->
                    if (requestId == searchRequestId) {
                        _searchState.value = ExplorerSearchState(
                            results = partialResults,
                            isSearching = true
                        )
                    }
                }
            ).fold(
                onSuccess = { items ->
                    if (requestId == searchRequestId) {
                        _searchState.value = ExplorerSearchState(results = items)
                    }
                },
                onFailure = { error ->
                    if (requestId == searchRequestId) {
                        _searchState.value = ExplorerSearchState(
                            error = error.localizedMessage ?: "Search failed"
                        )
                    }
                }
            )
        }
    }

    fun clearSearch() {
        searchRequestId++
        searchJob?.cancel()
        searchJob = null
        _searchState.value = ExplorerSearchState()
    }

    fun updateShowHiddenFilesPreference(showHidden: Boolean) {
        if (showHiddenFiles != showHidden) {
            showHiddenFiles = showHidden
            loadDirectory(activePath)
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
            val results = clip.items.map { item ->
                if (clip.isCut) {
                    fileRepository.move(item.path, activePath)
                } else {
                    fileRepository.copy(item.path, activePath)
                }
            }
            if (results.all { it.isSuccess } && clip.isCut) {
                activeClipboard = null
            }
            finishMutation(results, FileOperation.PASTE, "Unable to paste the selected items")
        }
    }

    private fun handleRename(item: FileItem, newName: String) {
        viewModelScope.launch {
            finishMutation(
                listOf(fileRepository.rename(item.path, newName)),
                FileOperation.RENAME,
                "Unable to rename ${item.name}"
            )
        }
    }

    private fun handleMoveToTrash(item: FileItem) {
        viewModelScope.launch {
            finishMutation(
                listOf(recycleBinRepository.moveToTrash(item)),
                FileOperation.MOVE_TO_TRASH,
                "Unable to move ${item.name} to Recycle Bin"
            )
        }
    }

    private fun handleBatchMoveToTrash(items: List<FileItem>) {
        viewModelScope.launch {
            val results = items.map { item ->
                recycleBinRepository.moveToTrash(item)
            }
            finishMutation(results, FileOperation.MOVE_TO_TRASH, "Unable to move all selected items to Recycle Bin")
        }
    }

    private fun handleDelete(item: FileItem) {
        viewModelScope.launch {
            finishMutation(
                listOf(fileRepository.delete(item.path)),
                FileOperation.DELETE,
                "Unable to delete ${item.name}"
            )
        }
    }

    private fun handleBatchDelete(items: List<FileItem>) {
        viewModelScope.launch {
            val results = items.map { item ->
                fileRepository.delete(item.path)
            }
            finishMutation(results, FileOperation.DELETE, "Unable to delete all selected items")
        }
    }

    private fun handleCreateFolder(name: String) {
        viewModelScope.launch {
            finishMutation(
                listOf(fileRepository.createFolder(activePath, name)),
                FileOperation.CREATE_FOLDER,
                "Unable to create folder $name"
            )
        }
    }

    private fun finishMutation(results: List<Result<Unit>>, operation: FileOperation, fallbackError: String) {
        val failedCount = results.count { it.isFailure }
        val succeededCount = results.size - failedCount
        val detail = results.firstNotNullOfOrNull { result ->
            result.exceptionOrNull()?.localizedMessage
        }?.takeIf { it.isNotBlank() } ?: if (results.any { it.isFailure }) fallbackError else null
        val failureMessage = when {
            failedCount == 0 -> null
            succeededCount > 0 -> "$succeededCount of ${results.size} items completed. $detail"
            else -> detail
        }
        _operationFeedback.tryEmit(OperationFeedback(operation, succeededCount, failedCount, detail))
        loadDirectory(activePath, isRefreshing = true, operationError = failureMessage)
    }

    private fun clearOperationError() {
        when (val state = _uiState.value) {
            is ExplorerUiState.Success -> _uiState.value = state.copy(operationError = null)
            is ExplorerUiState.Empty -> _uiState.value = state.copy(operationError = null)
            else -> Unit
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

    fun currentDirectoryPath(): String = activePath

    suspend fun listMoveTargets(path: String): Result<List<FileItem>> =
        fileRepository.getDirectoryContents(path, showHidden = showHiddenFiles, forceRefresh = true)
            .map { items -> items.filter(FileItem::isDirectory).sortedBy { it.name.lowercase(Locale.ROOT) } }

    suspend fun createMoveTargetFolder(parentPath: String, name: String): Result<Unit> =
        fileRepository.createFolder(parentPath, name)

    suspend fun moveItemsToFolder(items: List<FileItem>, targetPath: String): List<Result<Unit>> {
        val results = items.map { item ->
            val alreadyAtTarget = runCatching {
                File(item.path).parentFile?.canonicalPath == File(targetPath).canonicalPath
            }.getOrDefault(false)
            if (alreadyAtTarget) Result.success(Unit)
            else fileRepository.move(item.path, targetPath)
        }
        finishMutation(results, FileOperation.MOVE_MEDIA, "Unable to move the selected media")
        return results
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

    fun loadDirectory(
        path: String,
        isRefreshing: Boolean = false,
        operationError: String? = null
    ) {
        val requestId = ++directoryRequestId
        viewModelScope.launch {
            if (!fileRepository.hasStoragePermission()) {
                if (requestId != directoryRequestId) return@launch
                _uiState.value = ExplorerUiState.PermissionRequired
                return@launch
            }

            activePath = path
            val breadcrumbs = buildBreadcrumbs(path)

            // Fast SWR Path: Instantly render cached directory contents if available (0ms cold wait)
            val cached = fileRepository.getCachedContents(path, showHiddenFiles)
            if (requestId != directoryRequestId) return@launch
            if (cached != null) {
                val sortedCached = sortItems(cached, sortOption)
                _uiState.value = if (sortedCached.isEmpty()) {
                    ExplorerUiState.Empty(
                        currentPath = path,
                        breadcrumbs = breadcrumbs,
                        clipboard = activeClipboard,
                        showHiddenFiles = showHiddenFiles,
                        sortOption = sortOption,
                        operationError = operationError
                    )
                } else {
                    ExplorerUiState.Success(
                        currentPath = path,
                        breadcrumbs = breadcrumbs,
                        items = sortedCached,
                        isRefreshing = isRefreshing,
                        clipboard = activeClipboard,
                        showHiddenFiles = showHiddenFiles,
                        sortOption = sortOption,
                        operationError = operationError
                    )
                }
            } else if (!isRefreshing) {
                val currentState = _uiState.value
                if (currentState !is ExplorerUiState.Success || currentState.currentPath != path) {
                    _uiState.value = ExplorerUiState.Loading
                }
            }

            getDirectoryContentsUseCase(
                path = path,
                showHidden = showHiddenFiles,
                sortOption = sortOption,
                forceRefresh = isRefreshing
            ).fold(
                onSuccess = { items ->
                    if (requestId != directoryRequestId) return@fold
                    _uiState.value = if (items.isEmpty()) {
                        ExplorerUiState.Empty(
                            currentPath = path,
                            breadcrumbs = breadcrumbs,
                            clipboard = activeClipboard,
                            showHiddenFiles = showHiddenFiles,
                            sortOption = sortOption,
                            operationError = operationError
                        )
                    } else {
                        ExplorerUiState.Success(
                            currentPath = path,
                            breadcrumbs = breadcrumbs,
                            items = items,
                            isRefreshing = false,
                            clipboard = activeClipboard,
                            showHiddenFiles = showHiddenFiles,
                            sortOption = sortOption,
                            operationError = operationError
                        )
                    }
                    prefetchSubdirectories(items)
                },
                onFailure = { error ->
                    if (requestId != directoryRequestId) return@fold
                    val currentState = _uiState.value
                    if (currentState is ExplorerUiState.Success && currentState.currentPath == path) {
                        _uiState.value = currentState.copy(
                            isRefreshing = false,
                            operationError = operationError
                                ?: error.localizedMessage
                                ?: "Failed to refresh this folder"
                        )
                    } else {
                        _uiState.value = ExplorerUiState.Error(
                            message = error.localizedMessage ?: "Failed to read directory",
                            currentPath = path,
                            breadcrumbs = breadcrumbs
                        )
                    }
                }
            )
        }
    }

    private fun prefetchSubdirectories(items: List<FileItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            val subdirs = items.filter { it.isDirectory }.take(8)
            for (dir in subdirs) {
                if (fileRepository.getCachedContents(dir.path, showHiddenFiles) == null) {
                    fileRepository.getDirectoryContents(dir.path, showHiddenFiles, forceRefresh = false)
                }
            }
        }
    }

    private fun sortItems(items: List<FileItem>, sortOption: SortOption): List<FileItem> {
        val comparator = when (sortOption) {
            SortOption.NAME_ASC -> compareBy<FileItem> { it.name.lowercase(Locale.ROOT) }
            SortOption.NAME_DESC -> compareByDescending<FileItem> { it.name.lowercase(Locale.ROOT) }
            SortOption.DATE_DESC -> compareByDescending<FileItem> { it.lastModifiedMs }
            SortOption.DATE_ASC -> compareBy<FileItem> { it.lastModifiedMs }
            SortOption.SIZE_DESC -> compareByDescending<FileItem> { it.sizeBytes }
            SortOption.SIZE_ASC -> compareBy<FileItem> { it.sizeBytes }
        }
        return items.sortedWith(
            compareBy<FileItem> { !it.isDirectory }.then(comparator)
        )
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
            driveName: String,
            initialPath: String = rootPath,
            clipboardStore: FileClipboardStore
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val dataSource = AndroidFileLocalDataSource(appContext)
                val directoryCache = DirectoryCache.getInstance(appContext)
                val repository = FileRepositoryImpl(dataSource, directoryCache)
                val recycleBinRepository = RecycleBinRepositoryImpl.getInstance(appContext)
                val useCase = GetDirectoryContentsUseCase(repository)
                val searchUseCase = SearchFilesUseCase(repository)
                val explorerPreferences = ExplorerPreferences(appContext)
                return ExplorerViewModel(
                    rootPath = rootPath,
                    initialPath = initialPath,
                    driveName = driveName,
                    getDirectoryContentsUseCase = useCase,
                    fileRepository = repository,
                    recycleBinRepository = recycleBinRepository,
                    explorerPreferences = explorerPreferences,
                    searchFilesUseCase = searchUseCase,
                    clipboardStore = clipboardStore
                ) as T
            }
        }
    }
}
