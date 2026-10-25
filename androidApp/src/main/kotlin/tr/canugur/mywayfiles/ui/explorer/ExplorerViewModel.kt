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
import tr.canugur.mywayfiles.domain.model.ConflictResolution
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
    private val _activeOperation = MutableStateFlow<FileOperationProgress?>(null)
    val activeOperation: StateFlow<FileOperationProgress?> = _activeOperation.asStateFlow()
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
    private var activeMutationJob: Job? = null

    fun cancelCurrentOperation() {
        activeMutationJob?.cancel()
    }

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
            is ExplorerUiEvent.PasteWithResolution -> handlePaste(event.conflictResolution)
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

    private fun handlePaste(conflictResolution: ConflictResolution = ConflictResolution.AUTO_RENAME) {
        val clip = activeClipboard ?: return
        if (activeMutationJob?.isActive == true || _activeOperation.value != null) return
        val destinationPath = activePath
        // Clear active clipboard immediately so the bar is closed and cannot be double-tapped
        activeClipboard = null
        updateStateClipboard()

        runTrackedOperation(FileOperation.PASTE, clip.items) { item, report ->
            if (clip.isCut) fileRepository.move(item.path, destinationPath, conflictResolution, report)
            else fileRepository.copy(item.path, destinationPath, conflictResolution, report)
        }
    }

    private fun handleRename(item: FileItem, newName: String) {
        if (_activeOperation.value != null || activeMutationJob?.isActive == true) return
        viewModelScope.launch {
            finishMutation(
                listOf(fileRepository.rename(item.path, newName)),
                FileOperation.RENAME,
                "Unable to rename ${item.name}"
            )
        }
    }

    private fun handleMoveToTrash(item: FileItem) =
        runTrackedOperation(FileOperation.MOVE_TO_TRASH, listOf(item)) { selected, report ->
            recycleBinRepository.moveToTrash(selected, report)
        }

    private fun handleBatchMoveToTrash(items: List<FileItem>) =
        runTrackedOperation(FileOperation.MOVE_TO_TRASH, items) { item, report ->
            recycleBinRepository.moveToTrash(item, report)
        }

    private fun handleDelete(item: FileItem) =
        runTrackedOperation(FileOperation.DELETE, listOf(item)) { selected, _ ->
            fileRepository.delete(selected.path)
        }

    private fun handleBatchDelete(items: List<FileItem>) =
        runTrackedOperation(FileOperation.DELETE, items) { item, _ ->
            fileRepository.delete(item.path)
        }

    private fun runTrackedOperation(
        operation: FileOperation,
        items: List<FileItem>,
        action: suspend (FileItem, (Long, Long) -> Unit) -> Result<Unit>
    ) {
        if (items.isEmpty() || activeMutationJob?.isActive == true) return
        activeMutationJob = viewModelScope.launch {
            val results = mutableListOf<Result<Unit>>()
            try {
                items.forEachIndexed { index, item ->
                    _activeOperation.value = FileOperationProgress(
                        operation = operation,
                        itemName = item.name,
                        itemIndex = index,
                        itemCount = items.size,
                        canCancel = !item.isDirectory && operation in setOf(FileOperation.PASTE, FileOperation.MOVE_TO_TRASH)
                    )
                    results += try {
                        action(item) { copied, total ->
                            _activeOperation.value = FileOperationProgress(
                                operation = operation,
                                itemName = item.name,
                                itemIndex = index,
                                itemCount = items.size,
                                copiedBytes = copied,
                                totalBytes = total,
                                canCancel = !item.isDirectory && operation in setOf(FileOperation.PASTE, FileOperation.MOVE_TO_TRASH)
                            )
                        }
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Result.failure(error)
                    }
                }
                if (operation == FileOperation.PASTE) {
                    val clip = activeClipboard
                    if (clip != null) {
                        val remaining = clip.items.filterIndexed { index, _ -> results.getOrNull(index)?.isSuccess != true }
                        activeClipboard = remaining.takeIf { it.isNotEmpty() }?.let { clip.copy(items = it) }
                        updateStateClipboard()
                    }
                }
                finishMutation(results, operation, "Unable to complete ${operation.name.lowercase(Locale.ROOT)}")
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                val clip = activeClipboard
                if (operation == FileOperation.PASTE && clip != null) {
                    val remaining = clip.items.filterIndexed { index, _ -> results.getOrNull(index)?.isSuccess != true }
                    activeClipboard = remaining.takeIf { it.isNotEmpty() }?.let { clip.copy(items = it) }
                    updateStateClipboard()
                }
                finishMutation(
                    results,
                    operation,
                    "Unable to complete ${operation.name.lowercase(Locale.ROOT)}",
                    cancelled = true,
                    notProcessed = items.size - results.size
                )
                throw cancelled
            } finally {
                _activeOperation.value = null
            }
        }
    }

    private fun handleCreateFolder(name: String) {
        if (_activeOperation.value != null || activeMutationJob?.isActive == true) return
        viewModelScope.launch {
            finishMutation(
                listOf(fileRepository.createFolder(activePath, name)),
                FileOperation.CREATE_FOLDER,
                "Unable to create folder $name"
            )
        }
    }

    private fun finishMutation(
        results: List<Result<Unit>>,
        operation: FileOperation,
        fallbackError: String,
        cancelled: Boolean = false,
        notProcessed: Int = 0
    ) {
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
        _operationFeedback.tryEmit(
            OperationFeedback(
                operation = operation,
                succeeded = succeededCount,
                failed = failedCount,
                detail = detail,
                cancelled = cancelled,
                notProcessed = notProcessed
            )
        )
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

    private fun isAtExplorerRoot(): Boolean = runCatching {
        File(rootPath).canonicalPath == File(activePath).canonicalPath
    }.getOrDefault(rootPath == activePath)

    fun canNavigateUp(): Boolean = !isAtExplorerRoot()

    fun navigateBackWithinExplorer(): Boolean {
        if (isAtExplorerRoot()) return false
        if (navigateUp()) return true
        loadDirectory(rootPath)
        return true
    }

    private fun isAtOrBelowRoot(root: String, candidate: String): Boolean = runCatching {
        val normalizedRoot = File(root).canonicalPath.trimEnd(File.separatorChar)
        val normalizedCandidate = File(candidate).canonicalPath
        val rootPrefix = if (normalizedRoot.isEmpty()) File.separator else "$normalizedRoot${File.separator}"
        normalizedCandidate == normalizedRoot || normalizedCandidate.startsWith(rootPrefix)
    }.getOrDefault(false)

    private fun isStrictDescendant(root: String, candidate: String): Boolean =
        isAtOrBelowRoot(root, candidate) &&
            runCatching { File(root).canonicalPath != File(candidate).canonicalPath }.getOrDefault(false)

    fun currentDirectoryPath(): String = activePath

    suspend fun listMoveTargets(path: String): Result<List<FileItem>> =
        fileRepository.getDirectoryContents(path, showHidden = showHiddenFiles, forceRefresh = true)
            .map { items -> items.filter(FileItem::isDirectory).sortedBy { it.name.lowercase(Locale.ROOT) } }

    suspend fun createMoveTargetFolder(parentPath: String, name: String): Result<Unit> =
        fileRepository.createFolder(parentPath, name)

    suspend fun moveItemsToFolder(
        items: List<FileItem>,
        targetPath: String,
        conflictResolution: ConflictResolution = ConflictResolution.AUTO_RENAME,
        onProgress: (MoveProgress) -> Unit = {},
        onItemsMoved: (List<Pair<FileItem, String>>, String) -> Unit = { _, _ -> }
    ): List<Result<Unit>> {
        val results = mutableListOf<Result<Unit>>()
        try {
            items.forEachIndexed { index, item ->
                val alreadyAtTarget = runCatching {
                    File(item.path).parentFile?.canonicalPath == File(targetPath).canonicalPath
                }.getOrDefault(false)
                val result = if (alreadyAtTarget) {
                    Result.success(Unit)
                } else {
                    onProgress(MoveProgress(index, items.size, 0L, item.sizeBytes))
                    fileRepository.move(item.path, targetPath, conflictResolution) { copied, total ->
                        onProgress(MoveProgress(index, items.size, copied, total))
                    }
                }
                results += result
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            onItemsMoved(
                items.zip(results)
                    .filter { (_, result) -> result.isSuccess }
                    .map { (item, _) -> item to File(targetPath, item.name).path },
                targetPath
            )
            finishMutation(
                results,
                FileOperation.MOVE_MEDIA,
                "Unable to move the selected media",
                cancelled = true,
                notProcessed = items.size - results.size
            )
            throw cancelled
        }
        val successfullyMoved = items.zip(results)
            .filter { (_, result) -> result.isSuccess }
            .map { (item, _) -> item to File(targetPath, item.name).path }
        onItemsMoved(successfullyMoved, targetPath)
        val movedPaths = successfullyMoved.map { it.first.path }.toSet()
        val clip = activeClipboard
        if (clip != null) {
            val remaining = clip.items.filterNot { it.path in movedPaths }
            activeClipboard = remaining.takeIf { it.isNotEmpty() }?.let { clip.copy(items = it) }
            updateStateClipboard()
        }
        finishMutation(results, FileOperation.MOVE_MEDIA, "Unable to move the selected media")
        return results
    }

    fun navigateUp(): Boolean {
        if (!canNavigateUp()) return false
        val parent = File(activePath).parentFile ?: return false
        if (!isAtOrBelowRoot(rootPath, parent.path)) return false
        loadDirectory(parent.canonicalPath)
        return true
    }

    fun loadDirectory(
        path: String,
        isRefreshing: Boolean = false,
        operationError: String? = null
    ) {
        if (fileRepository.hasStoragePermission()) activePath = path
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
