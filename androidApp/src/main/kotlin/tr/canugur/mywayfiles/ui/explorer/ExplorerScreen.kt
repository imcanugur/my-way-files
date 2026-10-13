package tr.canugur.mywayfiles.ui.explorer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tr.canugur.mywayfiles.core.util.FileTypeClassifier
import tr.canugur.mywayfiles.domain.model.FileItem
import tr.canugur.mywayfiles.ui.explorer.components.EmptyFolderView
import tr.canugur.mywayfiles.ui.explorer.components.ExplorerTopBar
import tr.canugur.mywayfiles.ui.explorer.components.FileItemRow
import tr.canugur.mywayfiles.ui.explorer.components.PermissionCard
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
    var selectedImage by remember { mutableStateOf<FileItem?>(null) }

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

    BackHandler(enabled = selectedImage == null) {
        if (!viewModel.navigateUp()) {
            onNavigateBack()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ExplorerScreenContent(
            state = state,
            fallbackDriveName = driveName,
            fallbackRootPath = rootPath,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            onBackClick = {
                if (!viewModel.navigateUp()) {
                    onNavigateBack()
                }
            },
            onItemClick = { item ->
                if (item.isDirectory) {
                    viewModel.onEvent(ExplorerUiEvent.ItemClick(item))
                } else if (FileTypeClassifier.isImage(item)) {
                    selectedImage = item
                }
            },
            onEvent = viewModel::onEvent
        )

        // Integrated Full-Screen Image Viewer Overlay
        val activeImage = selectedImage
        AnimatedVisibility(
            visible = activeImage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (activeImage != null) {
                ImageViewerScreen(
                    fileItem = activeImage,
                    onDismiss = { selectedImage = null }
                )
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
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onBackClick: () -> Unit,
    onItemClick: (FileItem) -> Unit,
    onEvent: (ExplorerUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val breadcrumbs = when (state) {
        is ExplorerUiState.Success -> state.breadcrumbs
        is ExplorerUiState.Empty -> state.breadcrumbs
        is ExplorerUiState.Error -> state.breadcrumbs
        else -> listOf(Breadcrumb(title = fallbackDriveName, path = fallbackRootPath))
    }

    Scaffold(
        containerColor = AppColors.canvasBackground,
        topBar = {
            ExplorerTopBar(
                breadcrumbs = breadcrumbs,
                isDarkTheme = isDarkTheme,
                onBackClick = onBackClick,
                onBreadcrumbClick = { onEvent(ExplorerUiEvent.NavigateToBreadcrumb(it)) },
                onToggleTheme = onToggleTheme,
                onRefreshClick = { onEvent(ExplorerUiEvent.Refresh) }
            )
        },
        modifier = modifier
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyFolderView()
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
                    Text(
                        text = state.message,
                        color = AppColors.statusCritical,
                        fontSize = 13.sp
                    )
                }
            }

            is ExplorerUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        horizontal = Dimensions.SpacingDefault,
                        vertical = Dimensions.SpacingMedium
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingSmall)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        FileItemRow(
                            item = item,
                            onClick = { onItemClick(item) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(Dimensions.SpacingExtraLarge))
                    }
                }
            }
        }
    }
}
