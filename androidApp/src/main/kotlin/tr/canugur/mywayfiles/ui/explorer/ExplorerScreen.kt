package tr.canugur.mywayfiles.ui.explorer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.io.File
import tr.canugur.mywayfiles.core.util.FileTypeClassifier
import tr.canugur.mywayfiles.core.constants.SearchTiming
import tr.canugur.mywayfiles.core.util.FileShareHelper
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.domain.model.FileViewMode
import tr.canugur.mywayfiles.domain.model.SortOption
import tr.canugur.mywayfiles.data.preferences.SavedLocation
import tr.canugur.mywayfiles.ui.common.RetryErrorState
import tr.canugur.mywayfiles.ui.common.OperationErrorBanner
import tr.canugur.mywayfiles.ui.common.AppModalBottomSheet
import tr.canugur.mywayfiles.ui.explorer.components.*
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions
import tr.canugur.mywayfiles.ui.theme.MotionTokens
import tr.canugur.mywayfiles.ui.viewer.ImageViewerScreen
import tr.canugur.mywayfiles.ui.explorer.components.MoveTargetRoot
import tr.canugur.mywayfiles.ui.explorer.components.MoveToFolderDialog

/**
 * Stateful entry point for the Directory and File Explorer screen.
 */
@Composable
fun ExplorerScreen(
    rootPath: String,
    driveName: String,
    initialPath: String = rootPath,
    initialFilePath: String? = null,
    favoritePaths: Set<String> = emptySet(),
    startInSearch: Boolean = false,
    showHiddenFilesSetting: Boolean,
    confirmBeforeMovingToTrash: Boolean,
    autoHideViewerControls: Boolean,
    viewMode: FileViewMode,
    gridTileSize: Int,
    moveTargetRoots: List<MoveTargetRoot>,
    recentMoveTargets: List<SavedLocation>,
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onDirectoryVisited: (String, String, String, String) -> Unit,
    onToggleFavorite: (SavedLocation) -> Unit,
    onItemsMoved: (List<Pair<FileItem, String>>, String) -> Unit,
    clipboardStore: FileClipboardStore,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: ExplorerViewModel = viewModel(
        factory = ExplorerViewModel.provideFactory(
            context = context,
            rootPath = rootPath,
            driveName = driveName,
            initialPath = initialPath,
            clipboardStore = clipboardStore
        ),
        key = "$rootPath:$initialPath"
    )
    LaunchedEffect(showHiddenFilesSetting) {
        viewModel.updateShowHiddenFilesPreference(showHiddenFilesSetting)
    }
    LaunchedEffect(initialPath) {
        if (viewModel.currentDirectoryPath() != initialPath) viewModel.loadDirectory(initialPath)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }
    var isSearchActive by rememberSaveable(rootPath) { mutableStateOf(startInSearch) }
    var searchQuery by rememberSaveable(rootPath) { mutableStateOf("") }

    val currentPath = when (val s = state) {
        is ExplorerUiState.Success -> s.currentPath
        is ExplorerUiState.Empty -> s.currentPath
        is ExplorerUiState.Error -> s.currentPath
        ExplorerUiState.Loading -> viewModel.currentDirectoryPath()
        else -> initialPath
    }
    LaunchedEffect(currentPath) {
        isSelectionMode = false
        selectedPaths = emptySet()
        val folderName = File(currentPath).name.ifBlank { driveName }
        onDirectoryVisited(currentPath, folderName, rootPath, driveName)
    }

    val showHiddenFiles = when (val current = state) {
        is ExplorerUiState.Success -> current.showHiddenFiles
        is ExplorerUiState.Empty -> current.showHiddenFiles
        else -> false
    }
    // Do not key this effect by the full ExplorerUiState: it contains the entire file list,
    // so every state emission would compare a potentially large list and restart filesystem search.
    LaunchedEffect(isSearchActive, searchQuery, currentPath, showHiddenFiles) {
        if (isSearchActive && searchQuery.isNotBlank()) {
            delay(SearchTiming.QUERY_DEBOUNCE_MS)
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
    var itemToMoveToTrash by remember { mutableStateOf<FileItem?>(null) }
    var itemsToMoveToTrash by remember { mutableStateOf<List<FileItem>?>(null) }
    var itemsToMoveToFolder by remember { mutableStateOf<List<FileItem>?>(null) }

    fun requestMoveToTrash(item: FileItem) {
        if (confirmBeforeMovingToTrash) itemToMoveToTrash = item
        else viewModel.onEvent(ExplorerUiEvent.MoveToTrash(item))
    }

    fun requestBatchMoveToTrash(items: List<FileItem>) {
        if (confirmBeforeMovingToTrash) itemsToMoveToTrash = items
        else viewModel.onEvent(ExplorerUiEvent.BatchMoveToTrash(items))
        if (!confirmBeforeMovingToTrash) {
            isSelectionMode = false
            selectedPaths = emptySet()
        }
    }

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

    LaunchedEffect(initialFilePath, currentItems) {
        val targetPath = initialFilePath ?: return@LaunchedEffect
        val target = currentItems.firstOrNull { it.path == targetPath } ?: return@LaunchedEffect
        if (FileTypeClassifier.isImage(target) || FileTypeClassifier.isVideo(target)) {
            val index = mediaItems.indexOfFirst { it.path == targetPath }
            if (index >= 0) activeMediaIndex = index
        } else {
            itemForProperties = target
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ExplorerScreenContent(
            state = displayedState,
            fallbackDriveName = driveName,
            fallbackRootPath = rootPath,
            isSelectionMode = isSelectionMode,
            selectedPaths = selectedPaths,
            isSearchActive = isSearchActive,
            searchQuery = searchQuery,
            isSearchLoading = searchState.isSearching,
            searchError = searchState.error,
            onOpenDrawer = onOpenDrawer,
            favoritePaths = favoritePaths,
            onToggleFavorite = { item -> onToggleFavorite(SavedLocation(item.path, item.name, item.isDirectory)) },
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
            onBatchShare = {
                currentItems.singleOrNull { it.path in selectedPaths && !it.isDirectory }
                    ?.let { FileShareHelper.shareFile(context, it.path) }
            },
            onBatchCut = {
                val items = currentItems.filter { it.path in selectedPaths }
                viewModel.onEvent(ExplorerUiEvent.BatchCut(items))
                isSelectionMode = false
                selectedPaths = emptySet()
            },
            onBatchDelete = {
                val items = currentItems.filter { it.path in selectedPaths }
                requestBatchMoveToTrash(items)
            },
            onBatchMoveToFolder = {
                itemsToMoveToFolder = currentItems.filter {
                    it.path in selectedPaths && (FileTypeClassifier.isImage(it) || FileTypeClassifier.isVideo(it))
                }
            },
            onSortOptionSelected = { viewModel.onEvent(ExplorerUiEvent.ChangeSortOption(it)) },
            viewMode = viewMode,
            gridTileSize = gridTileSize,
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
            onDelete = ::requestMoveToTrash,
            onMoveToFolder = { itemsToMoveToFolder = listOf(it) },
            onProperties = { itemForProperties = it },
            onPaste = { viewModel.onEvent(ExplorerUiEvent.Paste) },
            onClearClipboard = { viewModel.onEvent(ExplorerUiEvent.ClearClipboard) },
            onEvent = viewModel::onEvent
        )

        // Integrated Full-Screen Unified Media Gallery Pager Overlay (Photos & Videos together)
        val currentMediaIndex = activeMediaIndex
        AnimatedVisibility(
            visible = currentMediaIndex != null && mediaItems.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(MotionTokens.NAVIGATION_ENTER_MS)) +
                scaleIn(
                    initialScale = 0.985f,
                    animationSpec = tween(MotionTokens.NAVIGATION_ENTER_MS, easing = FastOutSlowInEasing)
                ),
            exit = fadeOut(animationSpec = tween(MotionTokens.NAVIGATION_EXIT_MS)) +
                scaleOut(
                    targetScale = 0.985f,
                    animationSpec = tween(MotionTokens.NAVIGATION_EXIT_MS, easing = FastOutSlowInEasing)
                )
        ) {
            if (currentMediaIndex != null && mediaItems.isNotEmpty()) {
                ImageViewerScreen(
                    images = mediaItems,
                    initialIndex = currentMediaIndex,
                    autoHideControls = autoHideViewerControls,
                    onDelete = { item ->
                        requestMoveToTrash(item)
                    },
                    onCopy = { viewModel.onEvent(ExplorerUiEvent.Copy(it)) },
                    onMoveToFolder = { itemsToMoveToFolder = listOf(it) },
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

        val movingItems = itemsToMoveToFolder
        if (movingItems != null) {
            MoveToFolderDialog(
                itemsToMove = movingItems,
                roots = moveTargetRoots,
                recentTargets = recentMoveTargets,
                initialRootPath = rootPath,
                initialDirectoryPath = currentPath,
                onLoadFolders = viewModel::listMoveTargets,
                onCreateFolder = viewModel::createMoveTargetFolder,
                onMove = { items, targetPath ->
                    val results = viewModel.moveItemsToFolder(items, targetPath)
                    if (isSearchActive && searchQuery.isNotBlank()) viewModel.search(searchQuery)
                    onItemsMoved(
                        items.zip(results).filter { it.second.isSuccess }
                            .map { (item, _) -> item to File(targetPath, item.name).path },
                        targetPath
                    )
                    val movedPaths = items.zip(results).filter { it.second.isSuccess }.map { it.first.path }.toSet()
                    if (movedPaths.isNotEmpty()) selectedPaths = selectedPaths - movedPaths
                    if (results.isNotEmpty() && results.all { it.isSuccess }) {
                        isSelectionMode = false
                        selectedPaths = emptySet()
                    }
                    results
                },
                onDismiss = {
                    itemsToMoveToFolder = null
                }
            )
        }

        val trashItem = itemToMoveToTrash
        if (trashItem != null) {
            DeleteConfirmDialog(
                fileName = trashItem.name,
                onConfirm = {
                    viewModel.onEvent(ExplorerUiEvent.MoveToTrash(trashItem))
                    if (activeMediaIndex != null) activeMediaIndex = null
                    itemToMoveToTrash = null
                },
                onDismiss = { itemToMoveToTrash = null }
            )
        }

        val trashItems = itemsToMoveToTrash
        if (trashItems != null) {
            AppModalBottomSheet(onDismissRequest = { itemsToMoveToTrash = null }) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.SpacingLarge)
                        .padding(bottom = Dimensions.SpacingLarge),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
                ) {
                    Text(stringResource(R.string.dialog_move_to_trash_title), style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.batch_delete_confirm, trashItems.size))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        androidx.compose.material3.TextButton(onClick = { itemsToMoveToTrash = null }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        androidx.compose.material3.TextButton(onClick = {
                            viewModel.onEvent(ExplorerUiEvent.BatchMoveToTrash(trashItems))
                            itemsToMoveToTrash = null
                            isSelectionMode = false
                            selectedPaths = emptySet()
                        }) {
                            Text(stringResource(R.string.action_move_to_trash), color = AppColors.statusCritical)
                        }
                    }
                }
            }
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
    onOpenDrawer: () -> Unit,
    favoritePaths: Set<String>,
    onToggleFavorite: (FileItem) -> Unit,
    onBackClick: () -> Unit,
    onCreateFolderClick: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onCloseSelectionMode: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onBatchCopy: () -> Unit,
    onBatchShare: () -> Unit,
    onBatchCut: () -> Unit,
    onBatchDelete: () -> Unit,
    onBatchMoveToFolder: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    viewMode: FileViewMode,
    gridTileSize: Int,
    onItemClick: (FileItem) -> Unit,
    onLongClick: (FileItem) -> Unit,
    onToggleSelect: (FileItem) -> Unit,
    onCopy: (FileItem) -> Unit,
    onCut: (FileItem) -> Unit,
    onRename: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit,
    onMoveToFolder: (FileItem) -> Unit,
    onProperties: (FileItem) -> Unit,
    onPaste: () -> Unit,
    onClearClipboard: () -> Unit,
    onEvent: (ExplorerUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val contentOffsetPx = with(density) { MotionTokens.NAVIGATION_OFFSET_DP.dp.roundToPx() }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    var lastVisibleDirectoryState by remember { mutableStateOf<ExplorerUiState?>(null) }
    LaunchedEffect(state) {
        if (state is ExplorerUiState.Success || state is ExplorerUiState.Empty) {
            lastVisibleDirectoryState = state
        }
    }
    val contentState = if (state is ExplorerUiState.Loading) {
        lastVisibleDirectoryState ?: state
    } else state

    val breadcrumbs = when (contentState) {
        is ExplorerUiState.Success -> contentState.breadcrumbs
        is ExplorerUiState.Empty -> contentState.breadcrumbs
        is ExplorerUiState.Error -> contentState.breadcrumbs
        else -> listOf(Breadcrumb(title = fallbackDriveName, path = fallbackRootPath))
    }

    val sortOption = when (contentState) {
        is ExplorerUiState.Success -> contentState.sortOption
        is ExplorerUiState.Empty -> contentState.sortOption
        else -> SortOption.NAME_ASC
    }

    val currentItems = when (contentState) {
        is ExplorerUiState.Success -> contentState.items
        else -> emptyList()
    }
    val isAllSelected = currentItems.isNotEmpty() && selectedPaths.size >= currentItems.size

    PullToRefreshBox(
        isRefreshing = isSearchLoading ||
            (state as? ExplorerUiState.Success)?.isRefreshing == true ||
            (state is ExplorerUiState.Loading && contentState !== state),
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = AppColors.canvasBackground,
            topBar = {
                ExplorerTopBar(
                    breadcrumbs = breadcrumbs,
                    isSelectionMode = isSelectionMode,
                    selectedCount = selectedPaths.size,
                    isAllSelected = isAllSelected,
                    sortOption = sortOption,
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    onOpenSearch = onOpenSearch,
                    onSearchQueryChange = onSearchQueryChange,
                    onCloseSearch = onCloseSearch,
                    onOpenDrawer = onOpenDrawer,
                    onCreateFolderClick = onCreateFolderClick,
                    onEnterSelectionMode = onEnterSelectionMode,
                    onCloseSelectionMode = onCloseSelectionMode,
                    onToggleSelectAll = onToggleSelectAll,
                    onBatchCopy = onBatchCopy,
                    onBatchShare = onBatchShare,
                    canShareSelected = selectedPaths.size == 1 && currentItems.any {
                        it.path in selectedPaths && !it.isDirectory
                    },
                    canMoveSelected = currentItems.filter { it.path in selectedPaths }.let { selected ->
                        selected.isNotEmpty() && selected.all {
                            FileTypeClassifier.isImage(it) || FileTypeClassifier.isVideo(it)
                        }
                    },
                    onBatchMoveToFolder = onBatchMoveToFolder,
                    onBatchCut = onBatchCut,
                    onBatchDelete = onBatchDelete,
                    onSortOptionSelected = onSortOptionSelected,
                    onRefreshClick = onRefresh
                )
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = contentState,
                contentKey = { it.directoryMotionKey() },
                transitionSpec = {
                    val fromPath = initialState.directoryPathOrNull()
                    val toPath = targetState.directoryPathOrNull()
                    val backward = fromPath != null && toPath != null &&
                        isPathAncestor(toPath, fromPath)
                    val enterOffset = if (backward) -contentOffsetPx else contentOffsetPx
                    val exitOffset = -enterOffset
                    (slideInHorizontally(
                        initialOffsetX = { enterOffset },
                        animationSpec = tween(MotionTokens.CONTENT_ENTER_MS, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(MotionTokens.CONTENT_ENTER_MS)))
                        .togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { exitOffset },
                                animationSpec = tween(MotionTokens.CONTENT_EXIT_MS, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(MotionTokens.CONTENT_EXIT_MS))
                        )
                },
                modifier = Modifier.fillMaxSize(),
                label = "directory-content"
            ) { state ->
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

                        else -> {
                            if (viewMode == FileViewMode.GRID) {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = gridTileSize.dp),
                                    state = gridState,
                                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                                    contentPadding = PaddingValues(
                                        horizontal = Dimensions.SpacingLarge,
                                        vertical = Dimensions.SpacingSmall
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall),
                                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                                ) {
                                    if (isSearchActive && isSearchLoading && currentItems.isNotEmpty()) {
                                        item(key = "search-progress", span = { GridItemSpan(maxLineSpan) }) {
                                            LinearProgressIndicator(
                                                modifier = Modifier.fillMaxWidth(),
                                                color = AppColors.brandAccent
                                            )
                                        }
                                    }
                                    state.operationError?.let { message ->
                                        item(key = "operation-error", span = { GridItemSpan(maxLineSpan) }) {
                                            OperationErrorBanner(
                                                message = message,
                                                onDismiss = { onEvent(ExplorerUiEvent.ClearOperationError) }
                                            )
                                        }
                                    }
                                    item(key = "folder-content-heading", span = { GridItemSpan(maxLineSpan) }) {
                                        FolderContentsHeading(state.items.size)
                                    }
                                    items(
                                        items = state.items,
                                        key = { it.id },
                                        contentType = { if (it.isDirectory) "folder-tile" else "file-tile" }
                                    ) { item ->
                                        FileGridTile(
                                            item = item,
                                            isFavorite = item.path in favoritePaths,
                                            isSelectionMode = isSelectionMode,
                                            isSelected = item.path in selectedPaths,
                                            onClick = onItemClick,
                                            onLongClick = onLongClick,
                                            onToggleSelect = onToggleSelect,
                                            onCopy = onCopy,
                                            onCut = onCut,
                                            onShare = { FileShareHelper.shareFile(context, it.path) },
                                            onRename = onRename,
                                            onDelete = onDelete,
                                            onMoveToFolder = onMoveToFolder,
                                            onProperties = onProperties,
                                            onToggleFavorite = onToggleFavorite
                                        )
                                    }
                                    item(key = "bottom-space", span = { GridItemSpan(maxLineSpan) }) {
                                        Spacer(modifier = Modifier.height(Dimensions.SpacingHuge))
                                    }
                                }
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                                    contentPadding = PaddingValues(
                                        horizontal = Dimensions.SpacingLarge,
                                        vertical = Dimensions.SpacingSmall
                                    )
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
                                        FolderContentsHeading(state.items.size)
                                    }
                                    itemsIndexed(
                                        items = state.items,
                                        key = { _, item -> item.id },
                                        contentType = { _, item -> if (item.isDirectory) "folder-row" else "file-row" }
                                    ) { index, item ->
                                        FileItemRow(
                                            item = item,
                                            isFavorite = item.path in favoritePaths,
                                            isSelectionMode = isSelectionMode,
                                            isSelected = item.path in selectedPaths,
                                            compact = viewMode == FileViewMode.COMPACT,
                                            onClick = onItemClick,
                                            onLongClick = onLongClick,
                                            onToggleSelect = onToggleSelect,
                                            onCopy = onCopy,
                                            onCut = onCut,
                                            onShare = { FileShareHelper.shareFile(context, it.path) },
                                            onRename = onRename,
                                            onDelete = onDelete,
                                            onMoveToFolder = onMoveToFolder,
                                            onProperties = onProperties,
                                            onToggleFavorite = onToggleFavorite
                                        )
                                        if (index < state.items.lastIndex) {
                                            androidx.compose.material3.HorizontalDivider(color = AppColors.divider)
                                        }
                                    }
                                    item(key = "bottom-space") {
                                        Spacer(modifier = Modifier.height(Dimensions.SpacingHuge))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }

        // Clipboard actions are local state changes: show the destination bar immediately.
        val activeClipboard = when (contentState) {
            is ExplorerUiState.Success -> contentState.clipboard
            is ExplorerUiState.Empty -> contentState.clipboard
            else -> null
        }
        if (activeClipboard != null) {
            ClipboardBar(
                clipboard = activeClipboard,
                destinationPath = contentState.directoryPathOrNull() ?: fallbackRootPath,
                destinationName = breadcrumbs.lastOrNull()?.title ?: fallbackDriveName,
                onPaste = onPaste,
                onClear = onClearClipboard,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun FolderContentsHeading(itemCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimensions.SpacingDefault, bottom = Dimensions.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.folder_contents),
            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
            color = AppColors.textPrimary
        )
        Text(
            text = stringResource(R.string.items_count, itemCount),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary
        )
    }
}

private fun ExplorerUiState.directoryPathOrNull(): String? = when (this) {
    is ExplorerUiState.Success -> currentPath
    is ExplorerUiState.Empty -> currentPath
    is ExplorerUiState.Error -> currentPath
    else -> null
}

private fun ExplorerUiState.directoryMotionKey(): String =
    directoryPathOrNull()?.let { "directory:$it" } ?: when (this) {
        ExplorerUiState.Loading -> "loading"
        ExplorerUiState.PermissionRequired -> "permission"
        else -> "unavailable"
    }

private fun isPathAncestor(ancestor: String, descendant: String): Boolean {
    val normalizedAncestor = ancestor.trimEnd(File.separatorChar)
    return descendant != normalizedAncestor &&
        descendant.startsWith(normalizedAncestor + File.separator)
}
