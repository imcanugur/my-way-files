package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.SortOption
import tr.canugur.mywayfiles.ui.explorer.Breadcrumb
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions
import tr.canugur.mywayfiles.ui.theme.MotionTokens

/** Explorer app bar keeps the navigation drawer available at every directory depth. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerTopBar(
    breadcrumbs: List<Breadcrumb>,
    isSelectionMode: Boolean,
    selectedCount: Int,
    isAllSelected: Boolean,
    sortOption: SortOption,
    isSearchActive: Boolean,
    searchQuery: String,
    onOpenSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onNavigateUp: () -> Unit,
    onOpenDrawer: () -> Unit,
    onCreateFolderClick: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onCloseSelectionMode: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var optionsMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Surface(
        color = AppColors.canvasBackground,
        modifier = modifier.fillMaxWidth().statusBarsPadding()
    ) {
        AnimatedContent(
            targetState = when {
                isSelectionMode -> "selection"
                isSearchActive -> "search"
                else -> "browse"
            },
            transitionSpec = {
                (fadeIn(animationSpec = tween(MotionTokens.CONTENT_ENTER_MS)) +
                    slideInVertically(
                        initialOffsetY = { it / 5 },
                        animationSpec = tween(MotionTokens.CONTENT_ENTER_MS)
                    )).togetherWith(
                    fadeOut(animationSpec = tween(MotionTokens.CONTENT_EXIT_MS)) +
                        slideOutVertically(
                            targetOffsetY = { -it / 5 },
                            animationSpec = tween(MotionTokens.CONTENT_EXIT_MS)
                        )
                )
            },
            label = "explorer-toolbar-state"
        ) { toolbarState ->
        when (toolbarState) {
        "selection" -> {
            ExplorerSelectionTopBar(
                selectedCount = selectedCount,
                isAllSelected = isAllSelected,
                onCloseSelection = onCloseSelectionMode,
                onToggleSelectAll = onToggleSelectAll,
                modifier = Modifier.fillMaxWidth()
            )
        }
        "search" -> {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onCloseSearch) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.close_search),
                            tint = AppColors.textPrimary
                        )
                    }
                },
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.search_this_location)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close),
                                        contentDescription = stringResource(R.string.clear_search),
                                        tint = AppColors.textSecondary
                                    )
                                }
                            }
                        }
                    )
                },
                actions = {},
                modifier = Modifier.fillMaxWidth(),
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.canvasBackground)
            )
        }
        else -> {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compactToolbar = maxWidth < Dimensions.CompactActionBreakpoint
                TopAppBar(
                navigationIcon = {
                    if (breadcrumbs.size > 1) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.navigate_up),
                                tint = AppColors.textPrimary
                            )
                        }
                    } else {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(
                                painter = painterResource(R.drawable.ic_menu),
                                contentDescription = stringResource(R.string.open_navigation),
                                tint = AppColors.textPrimary
                            )
                        }
                    }
                },
                title = {
                    AnimatedContent(
                        targetState = breadcrumbs.lastOrNull()?.title.orEmpty(),
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(MotionTokens.CONTENT_ENTER_MS)) +
                                slideInVertically(
                                    initialOffsetY = { it / 5 },
                                    animationSpec = tween(MotionTokens.CONTENT_ENTER_MS)
                                )).togetherWith(
                                fadeOut(animationSpec = tween(MotionTokens.CONTENT_EXIT_MS)) +
                                    slideOutVertically(
                                        targetOffsetY = { -it / 5 },
                                        animationSpec = tween(MotionTokens.CONTENT_EXIT_MS)
                                    )
                            )
                        },
                        label = "explorer-folder-title"
                    ) { title ->
                        Text(
                            text = title,
                            style = if (compactToolbar) MaterialTheme.typography.titleMedium
                            else MaterialTheme.typography.titleLarge,
                            color = AppColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = stringResource(R.string.search_files),
                            tint = AppColors.textSecondary
                        )
                    }
                    if (!compactToolbar) {
                        SortMenuAnchor(
                            expanded = sortMenuExpanded,
                            onExpand = { sortMenuExpanded = true },
                            onDismiss = { sortMenuExpanded = false },
                            sortOption = sortOption,
                            onCreateFolderClick = onCreateFolderClick,
                            onEnterSelectionMode = onEnterSelectionMode,
                            onSortOptionSelected = onSortOptionSelected,
                            onRefreshClick = onRefreshClick
                        )
                    }
                    ExplorerOverflowAnchor(
                        expanded = optionsMenuExpanded,
                        onExpand = { optionsMenuExpanded = true },
                        onDismiss = { optionsMenuExpanded = false },
                        sortOption = sortOption,
                        onCreateFolderClick = onCreateFolderClick,
                        onEnterSelectionMode = onEnterSelectionMode,
                        onSortOptionSelected = onSortOptionSelected,
                        onRefreshClick = onRefreshClick,
                        onOpenDrawer = onOpenDrawer
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.canvasBackground)
                )
            }

        }
        }
        }
    }
}

@Composable
private fun ExplorerOverflowAnchor(
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    sortOption: SortOption,
    onCreateFolderClick: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onRefreshClick: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    androidx.compose.foundation.layout.Box {
        IconButton(onClick = onExpand) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.options_menu),
                tint = AppColors.textPrimary
            )
        }
        ExplorerOptionsMenu(
            expanded = expanded,
            currentSortOption = sortOption,
            onDismissRequest = onDismiss,
            onCreateFolderClick = onCreateFolderClick,
            onEnterSelectionMode = onEnterSelectionMode,
            onSortOptionSelected = onSortOptionSelected,
            onRefreshClick = onRefreshClick,
            onOpenDrawer = onOpenDrawer
        )
    }
}

@Composable
private fun SortMenuAnchor(
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    sortOption: SortOption,
    onCreateFolderClick: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onRefreshClick: () -> Unit
) {
    androidx.compose.foundation.layout.Box {
        IconButton(onClick = onExpand) {
            Icon(
                painter = painterResource(R.drawable.ic_sort),
                contentDescription = stringResource(R.string.sort_by),
                tint = AppColors.textSecondary
            )
        }
        ExplorerOptionsMenu(
            expanded = expanded,
            currentSortOption = sortOption,
            onDismissRequest = onDismiss,
            onCreateFolderClick = onCreateFolderClick,
            onEnterSelectionMode = onEnterSelectionMode,
            onSortOptionSelected = onSortOptionSelected,
            onRefreshClick = onRefreshClick,
            sortOnly = true
        )
    }
}
