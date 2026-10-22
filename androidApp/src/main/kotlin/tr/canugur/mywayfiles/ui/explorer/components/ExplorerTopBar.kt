package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.SortOption
import tr.canugur.mywayfiles.ui.explorer.Breadcrumb
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/** Explorer app bar: current directory first, with its navigable ancestors directly below. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerTopBar(
    breadcrumbs: List<Breadcrumb>,
    isDarkTheme: Boolean,
    isSelectionMode: Boolean,
    selectedCount: Int,
    isAllSelected: Boolean,
    showHiddenFiles: Boolean,
    sortOption: SortOption,
    isSearchActive: Boolean,
    searchQuery: String,
    onOpenSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onBackClick: () -> Unit,
    onBreadcrumbClick: (String) -> Unit,
    onCreateFolderClick: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onCloseSelectionMode: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onBatchCopy: () -> Unit,
    onBatchShare: () -> Unit,
    canShareSelected: Boolean,
    onBatchCut: () -> Unit,
    onBatchDelete: () -> Unit,
    onToggleHiddenFiles: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onToggleTheme: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var optionsMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val parentBreadcrumbs = breadcrumbs.dropLast(1)

    LaunchedEffect(breadcrumbs) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }
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
        if (isSelectionMode) {
            ExplorerSelectionTopBar(
                selectedCount = selectedCount,
                isAllSelected = isAllSelected,
                onCloseSelection = onCloseSelectionMode,
                onToggleSelectAll = onToggleSelectAll,
                onBatchCopy = onBatchCopy,
                onBatchShare = onBatchShare,
                canShareSelected = canShareSelected,
                onBatchCut = onBatchCut,
                onBatchDelete = onBatchDelete,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (isSearchActive) {
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
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compactToolbar = maxWidth < Dimensions.CompactActionBreakpoint
                TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_up),
                            tint = AppColors.textPrimary
                        )
                    }
                },
                title = {
                    Text(
                        text = breadcrumbs.lastOrNull()?.title.orEmpty(),
                        style = if (compactToolbar) MaterialTheme.typography.titleMedium
                        else MaterialTheme.typography.titleLarge,
                        color = AppColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                            showHiddenFiles = showHiddenFiles,
                            sortOption = sortOption,
                            onCreateFolderClick = onCreateFolderClick,
                            onEnterSelectionMode = onEnterSelectionMode,
                            onToggleHiddenFiles = onToggleHiddenFiles,
                            onSortOptionSelected = onSortOptionSelected,
                            onRefreshClick = onRefreshClick
                        )
                    }
                    ExplorerOverflowAnchor(
                        expanded = optionsMenuExpanded,
                        onExpand = { optionsMenuExpanded = true },
                        onDismiss = { optionsMenuExpanded = false },
                        showHiddenFiles = showHiddenFiles,
                        sortOption = sortOption,
                        onCreateFolderClick = onCreateFolderClick,
                        onEnterSelectionMode = onEnterSelectionMode,
                        onToggleHiddenFiles = onToggleHiddenFiles,
                        onSortOptionSelected = onSortOptionSelected,
                        onRefreshClick = onRefreshClick,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = onToggleTheme
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.canvasBackground)
                )
            }

            if (parentBreadcrumbs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(
                            start = Dimensions.ActionButtonSize + Dimensions.SpacingLarge,
                            end = Dimensions.SpacingSmall
                        ),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingExtraSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    parentBreadcrumbs.forEachIndexed { index, crumb ->
                        if (index > 0) {
                            Text("›", color = AppColors.textMuted, fontSize = 14.sp)
                        }
                        Text(
                            text = crumb.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = AppColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .heightIn(min = Dimensions.ActionButtonSize)
                                .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                                .clickable(role = Role.Button) { onBreadcrumbClick(crumb.path) }
                                .padding(horizontal = Dimensions.SpacingSmall)
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
    showHiddenFiles: Boolean,
    sortOption: SortOption,
    onCreateFolderClick: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onToggleHiddenFiles: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    onRefreshClick: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
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
            showHiddenFiles = showHiddenFiles,
            currentSortOption = sortOption,
            onDismissRequest = onDismiss,
            onCreateFolderClick = onCreateFolderClick,
            onEnterSelectionMode = onEnterSelectionMode,
            onToggleHiddenFiles = onToggleHiddenFiles,
            onSortOptionSelected = onSortOptionSelected,
            onRefreshClick = onRefreshClick,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme
        )
    }
}

@Composable
private fun SortMenuAnchor(
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    showHiddenFiles: Boolean,
    sortOption: SortOption,
    onCreateFolderClick: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onToggleHiddenFiles: () -> Unit,
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
            showHiddenFiles = showHiddenFiles,
            currentSortOption = sortOption,
            onDismissRequest = onDismiss,
            onCreateFolderClick = onCreateFolderClick,
            onEnterSelectionMode = onEnterSelectionMode,
            onToggleHiddenFiles = onToggleHiddenFiles,
            onSortOptionSelected = onSortOptionSelected,
            onRefreshClick = onRefreshClick,
            sortOnly = true
        )
    }
}
