package tr.canugur.mywayfiles.ui.explorer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import tr.canugur.mywayfiles.core.util.FileTypeClassifier
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.domain.model.SortOption
import tr.canugur.mywayfiles.ui.common.RetryErrorState
import tr.canugur.mywayfiles.ui.common.OperationErrorBanner
import tr.canugur.mywayfiles.ui.explorer.components.*
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions
import tr.canugur.mywayfiles.ui.viewer.ImageViewerScreen

/**
 * Stateful entry point for the Directory and File Explorer screen.
 */
@Composable
fun ExplorerScreen(
    rootPath: String,
    driveName: String,
    startInSearch: Boolean = false,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: ExplorerViewModel = viewModel(
        factory = ExplorerViewModel.provideFactory(
            context = context,
            rootPath = rootPath,
            driveName = driveName
        ),
        key = rootPath
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }
    var isSearchActive by rememberSaveable(rootPath) { mutableStateOf(startInSearch) }
    var searchQuery by rememberSaveable(rootPath) { mutableStateOf("") }

    val currentPath = when (val s = state) {
        is ExplorerUiState.Success -> s.currentPath
        is ExplorerUiState.Empty -> s.currentPath
        else -> rootPath
    }
    LaunchedEffect(currentPath) {
        isSelectionMode = false
        selectedPaths = emptySet()
    }

    val showHiddenFiles = when (val current = state) {
        is ExplorerUiState.Success -> current.showHiddenFiles
        is ExplorerUiState.Empty -> current.showHiddenFiles
        else -> false
    }
    LaunchedEffect(isSearchActive, searchQuery, currentPath, showHiddenFiles, state) {
        if (isSearchActive && searchQuery.isNotBlank()) {
            delay(250)
            viewModel.search(searchQuery)
        } else {
            viewModel.clearSearch()
        }
    }

    fun closeSearch() {
        isSearchActive = false
        searchQuery = ""
        viewModel.clearSearch()
    }

    val displayedItems = when {
        isSearchActive && searchQuery.isNotBlank() -> searchState.results
        state is ExplorerUiState.Success -> (state as ExplorerUiState.Success).items
        else -> emptyList()
    }
    val displayedState = when {
        isSearchActive && searchQuery.isNotBlank() && state is ExplorerUiState.Success ->
            (state as ExplorerUiState.Success).copy(items = displayedItems)
        isSearchActive && searchQuery.isNotBlank() && state is ExplorerUiState.Empty -> {
            val emptyState = state as ExplorerUiState.Empty
            ExplorerUiState.Success(
                currentPath = emptyState.currentPath,
                breadcrumbs = emptyState.breadcrumbs,
                items = displayedItems,
                clipboard = emptyState.clipboard,
                showHiddenFiles = emptyState.showHiddenFiles,
                sortOption = emptyState.sortOption,
                operationError = emptyState.operationError
            )
        }
        else -> state
    }

    val mediaItems = remember(displayedState) {
        val currentState = displayedState
        if (currentState is ExplorerUiState.Success) {
            currentState.items.filter { FileTypeClassifier.isImage(it) || FileTypeClassifier.isVideo(it) }
        } else emptyList()
    }
    var activeMediaIndex by remember { mutableStateOf<Int?>(null) }
    var itemToRename by remember { mutableStateOf<FileItem?>(null) }
    var itemForProperties by remember { mutableStateOf<FileItem?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onEvent(ExplorerUiEvent.CheckPermission)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(enabled = isSelectionMode || activeMediaIndex == null) {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedPaths = emptySet()
        } else if (isSearchActive) {
            closeSearch()
        } else if (!viewModel.navigateUp()) {
            onNavigateBack()
        }
    }

    val currentItems = when (val s = displayedState) {
        is ExplorerUiState.Success -> s.items
        else -> emptyList()
    }

    Box(modifier = modifier.fillMaxSize()) {
        ExplorerScreenContent(
            state = displayedState,
            fallbackDriveName = driveName,
            fallbackRootPath = rootPath,
            isDarkTheme = isDarkTheme,
            isSelectionMode = isSelectionMode,
            selectedPaths = selectedPaths,
            isSearchActive = isSearchActive,
            searchQuery = searchQuery,
            isSearchLoading = searchState.isSearching,
            searchError = searchState.error,
            onToggleTheme = onToggleTheme,
            onBackClick = {
                if (isSelectionMode) {
                    isSelectionMode = false
                    selectedPaths = emptySet()
                } else if (isSearchActive) {
                    closeSearch()
                } else if (!viewModel.navigateUp()) {
                    onNavigateBack()
                }
            },
            onOpenSearch = {
                searchQuery = ""
                viewModel.clearSearch()
                isSearchActive = true
            },
            onSearchQueryChange = { query ->
                searchQuery = query
                viewModel.clearSearch()
            },
            onCloseSearch = ::closeSearch,
            onSearchRetry = { viewModel.search(searchQuery) },
            onRefresh = {
                if (isSearchActive && searchQuery.isNotBlank()) viewModel.search(searchQuery)
                else viewModel.onEvent(ExplorerUiEvent.Refresh)
            },
            onCreateFolderClick = { showCreateFolderDialog = true },
            onEnterSelectionMode = { isSelectionMode = true },
            onCloseSelectionMode = {
                isSelectionMode = false
                selectedPaths = emptySet()
            },
            onToggleSelectAll = {
                selectedPaths = if (selectedPaths.size >= currentItems.size) {
                    emptySet()
                } else {
                    currentItems.map { it.path }.toSet()
                }
            },
            onBatchCopy = {
                val items = currentItems.filter { it.path in selectedPaths }
                viewModel.onEvent(ExplorerUiEvent.BatchCopy(items))
                isSelectionMode = false
                selectedPaths = emptySet()
            },
            onBatchCut = {
                val items = currentItems.filter { it.path in selectedPaths }
                viewModel.onEvent(ExplorerUiEvent.BatchCut(items))
                isSelectionMode = false
                selectedPaths = emptySet()
            },
            onBatchDelete = {
                val items = currentItems.filter { it.path in selectedPaths }
                viewModel.onEvent(ExplorerUiEvent.BatchMoveToTrash(items))
                isSelectionMode = false
                selectedPaths = emptySet()
            },
            onToggleHiddenFiles = { viewModel.onEvent(ExplorerUiEvent.ToggleShowHiddenFiles) },
            onSortOptionSelected = { viewModel.onEvent(ExplorerUiEvent.ChangeSortOption(it)) },
            onItemClick = { item ->
                if (isSelectionMode) {
                    selectedPaths = if (item.path in selectedPaths) {
                        selectedPaths - item.path
                    } else {
                        selectedPaths + item.path
                    }
                } else if (item.isDirectory) {
                    if (isSearchActive) closeSearch()
                    viewModel.onEvent(ExplorerUiEvent.ItemClick(item))
                } else if (FileTypeClassifier.isImage(item) || FileTypeClassifier.isVideo(item)) {
                    val idx = mediaItems.indexOfFirst { it.path == item.path }
                    if (idx >= 0) {
                        activeMediaIndex = idx
                    }
                } else {
                    // Unsupported file types have no inline viewer; expose their available details instead.
                    itemForProperties = item
                }
            },
            onLongClick = { item ->
                isSelectionMode = true
                selectedPaths = selectedPaths + item.path
            },
            onToggleSelect = { item ->
                selectedPaths = if (item.path in selectedPaths) {
                    selectedPaths - item.path
                } else {
                    selectedPaths + item.path
                }
            },
            onCopy = { viewModel.onEvent(ExplorerUiEvent.Copy(it)) },
            onCut = { viewModel.onEvent(ExplorerUiEvent.Cut(it)) },
            onRename = { itemToRename = it },
            onDelete = { viewModel.onEvent(ExplorerUiEvent.MoveToTrash(it)) },
            onProperties = { itemForProperties = it },
            onPaste = { viewModel.onEvent(ExplorerUiEvent.Paste) },
            onClearClipboard = { viewModel.onEvent(ExplorerUiEvent.ClearClipboard) },
            onEvent = viewModel::onEvent
        )

        // Integrated Full-Screen Unified Media Gallery Pager Overlay (Photos & Videos together)
        val currentMediaIndex = activeMediaIndex
        AnimatedVisibility(
            visible = currentMediaIndex != null && mediaItems.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (currentMediaIndex != null && mediaItems.isNotEmpty()) {
                ImageViewerScreen(
                    images = mediaItems,
                    initialIndex = currentMediaIndex,
                    onDelete = { item ->
                        viewModel.onEvent(ExplorerUiEvent.MoveToTrash(item))
                        if (mediaItems.size <= 1) {
                            activeMediaIndex = null
                        } else if (currentMediaIndex >= mediaItems.size - 1) {
                            activeMediaIndex = (mediaItems.size - 2).coerceAtLeast(0)
                        }
                    },
                    onCopy = { viewModel.onEvent(ExplorerUiEvent.Copy(it)) },
                    onDismiss = {
                        activeMediaIndex = null
                    }
                )
            }
        }

        // Operation Dialogs
        val renameItem = itemToRename
        if (renameItem != null) {
            RenameDialog(
                initialName = renameItem.name,
                onConfirm = { viewModel.onEvent(ExplorerUiEvent.Rename(renameItem, it)) },
                onDismiss = { itemToRename = null }
            )
        }

        val propItem = itemForProperties
        if (propItem != null) {
            PropertiesDialog(
                item = propItem,
                onDismiss = { itemForProperties = null }
            )
        }

        if (showCreateFolderDialog) {
            CreateFolderDialog(
                onConfirm = { viewModel.onEvent(ExplorerUiEvent.CreateFolder(it)) },
                onDismiss = { showCreateFolderDialog = false }
            )
        }
    }
}

/**
 * Pure Stateless UI presentation driven strictly by unidirectional data flow.
 */
@Composable
fun ExplorerScreenContent(
    state: ExplorerUiState,
    fallbackDriveName: String,
    fallbackRootPath: String,
    isDarkTheme: Boolean,
    isSelectionMode: Boolean,
    selectedPaths: Set<String>,
    isSearchActive: Boolean,
    searchQuery: String,
    isSearchLoading: Boolean,
    searchError: String?,
    onOpenSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onSearchRetry: () -> Unit,
    onRefresh: () -> Unit,
    onToggleTheme: () -> Unit,
    onBackClick: () -> Unit,
    onCreateFolderClick: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onCloseSelectionMode: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onBatchCopy: () -> Unit,
    onBatchCut: () -> Unit,
    onBatchDelete: () -> Unit,
    onToggleHiddenFiles: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onItemClick: (FileItem) -> Unit,
    onLongClick: (FileItem) -> Unit,
    onToggleSelect: (FileItem) -> Unit,
    onCopy: (FileItem) -> Unit,
    onCut: (FileItem) -> Unit,
    onRename: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit,
    onProperties: (FileItem) -> Unit,
    onPaste: () -> Unit,
    onClearClipboard: () -> Unit,
    onEvent: (ExplorerUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val breadcrumbs = when (state) {
        is ExplorerUiState.Success -> state.breadcrumbs
        is ExplorerUiState.Empty -> state.breadcrumbs
        is ExplorerUiState.Error -> state.breadcrumbs
        else -> listOf(Breadcrumb(title = fallbackDriveName, path = fallbackRootPath))
    }

    val showHiddenFiles = when (state) {
        is ExplorerUiState.Success -> state.showHiddenFiles
        is ExplorerUiState.Empty -> state.showHiddenFiles
        else -> false
    }

    val sortOption = when (state) {
        is ExplorerUiState.Success -> state.sortOption
        is ExplorerUiState.Empty -> state.sortOption
        else -> SortOption.NAME_ASC
    }

    val currentItems = when (state) {
        is ExplorerUiState.Success -> state.items
        else -> emptyList()
    }
    val isAllSelected = currentItems.isNotEmpty() && selectedPaths.size >= currentItems.size

    PullToRefreshBox(
        isRefreshing = isSearchLoading || (state as? ExplorerUiState.Success)?.isRefreshing == true,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = AppColors.canvasBackground,
            topBar = {
                ExplorerTopBar(
                    breadcrumbs = breadcrumbs,
                    isDarkTheme = isDarkTheme,
                    isSelectionMode = isSelectionMode,
                    selectedCount = selectedPaths.size,
                    isAllSelected = isAllSelected,
                    showHiddenFiles = showHiddenFiles,
                    sortOption = sortOption,
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    onOpenSearch = onOpenSearch,
                    onSearchQueryChange = onSearchQueryChange,
                    onCloseSearch = onCloseSearch,
                    onBackClick = onBackClick,
                    onBreadcrumbClick = {
                        if (isSearchActive) onCloseSearch()
                        onEvent(ExplorerUiEvent.NavigateToBreadcrumb(it))
                    },
                    onCreateFolderClick = onCreateFolderClick,
                    onEnterSelectionMode = onEnterSelectionMode,
                    onCloseSelectionMode = onCloseSelectionMode,
                    onToggleSelectAll = onToggleSelectAll,
                    onBatchCopy = onBatchCopy,
                    onBatchCut = onBatchCut,
                    onBatchDelete = onBatchDelete,
                    onToggleHiddenFiles = onToggleHiddenFiles,
                    onSortOptionSelected = onSortOptionSelected,
                    onToggleTheme = onToggleTheme,
                    onRefreshClick = onRefresh
                )
            }
        ) { innerPadding ->
            when (state) {
                is ExplorerUiState.Loading -> {
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

                is ExplorerUiState.PermissionRequired -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(Dimensions.SpacingLarge),
                        contentAlignment = Alignment.Center
                    ) {
                        PermissionCard()
                    }
                }

                is ExplorerUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        state.operationError?.let { message ->
                            OperationErrorBanner(
                                message = message,
                                onDismiss = { onEvent(ExplorerUiEvent.ClearOperationError) },
                                modifier = Modifier.padding(
                                    horizontal = Dimensions.SpacingDefault,
                                    vertical = Dimensions.SpacingSmall
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyFolderView()
                        }
                    }
                }

                is ExplorerUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(Dimensions.SpacingLarge),
                        contentAlignment = Alignment.Center
                    ) {
                        RetryErrorState(
                            message = state.message,
                            onRetry = { onEvent(ExplorerUiEvent.Refresh) }
                        )
                    }
                }

                is ExplorerUiState.Success -> {
                    when {
                        isSearchActive && searchQuery.isNotBlank() && searchError != null -> {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(Dimensions.SpacingLarge),
                                contentAlignment = Alignment.Center
                            ) {
                                RetryErrorState(message = searchError, onRetry = onSearchRetry)
                            }
                        }

                        isSearchActive && searchQuery.isNotBlank() && isSearchLoading && currentItems.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AppColors.brandAccent)
                            }
                        }

                        isSearchActive && searchQuery.isNotBlank() && currentItems.isEmpty() -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(Dimensions.SpacingLarge),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(tr.canugur.mywayfiles.R.string.search_no_results),
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                    color = AppColors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(Dimensions.SpacingSmall))
                                Text(
                                    text = stringResource(tr.canugur.mywayfiles.R.string.search_no_results_hint),
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                    color = AppColors.textSecondary
                                )
                            }
                        }

                        else -> LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(
                            horizontal = Dimensions.SpacingLarge,
                            vertical = Dimensions.SpacingSmall
                        ),
                    ) {
                        if (isSearchActive && isSearchLoading && currentItems.isNotEmpty()) {
                            item(key = "search-progress") {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = AppColors.brandAccent
                                )
                            }
                        }
                        state.operationError?.let { message ->
                            item(key = "operation-error") {
                                OperationErrorBanner(
                                    message = message,
                                    onDismiss = { onEvent(ExplorerUiEvent.ClearOperationError) }
                                )
                            }
                        }

                        item(key = "folder-content-heading") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = Dimensions.SpacingDefault, bottom = Dimensions.SpacingSmall),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(tr.canugur.mywayfiles.R.string.folder_contents),
                                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                                    color = AppColors.textPrimary
                                )
                                Text(
                                    text = stringResource(tr.canugur.mywayfiles.R.string.items_count, state.items.size),
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                    color = AppColors.textSecondary
                                )
                            }
                        }
                        itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
                            FileItemRow(
                                item = item,
                                isSelectionMode = isSelectionMode,
                                isSelected = item.path in selectedPaths,
                                onClick = { onItemClick(item) },
                                onLongClick = onLongClick,
                                onToggleSelect = onToggleSelect,
                                onCopy = onCopy,
                                onCut = onCut,
                                onRename = onRename,
                                onDelete = onDelete,
                                onProperties = onProperties
                            )
                            if (index < state.items.lastIndex) {
                                androidx.compose.material3.HorizontalDivider(color = AppColors.divider)
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(Dimensions.SpacingHuge))
                        }
                    }
                    }
                }
            }
        }

        // Floating Clipboard Banner
        val activeClipboard = when (state) {
            is ExplorerUiState.Success -> state.clipboard
            is ExplorerUiState.Empty -> state.clipboard
            else -> null
        }
        if (activeClipboard != null) {
            ClipboardBar(
                clipboard = activeClipboard,
                onPaste = onPaste,
                onClear = onClearClipboard,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Dimensions.SpacingDefault)
            )
        }
    }
}
