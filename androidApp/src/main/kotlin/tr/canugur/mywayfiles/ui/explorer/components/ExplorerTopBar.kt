package tr.canugur.mywayfiles.ui.explorer.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.SortOption
import tr.canugur.mywayfiles.ui.explorer.Breadcrumb
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/** Mobile-first file browser app bar with lightweight location breadcrumbs. */
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
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val parentBreadcrumbs = breadcrumbs.dropLast(1)

    LaunchedEffect(breadcrumbs.size) { scrollState.animateScrollTo(scrollState.maxValue) }
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SpacingSmall, vertical = Dimensions.SpacingExtraSmall)
        ) {
            if (isSelectionMode) {
                ExplorerSelectionTopBar(
                    selectedCount = selectedCount,
                    isAllSelected = isAllSelected,
                    onCloseSelection = onCloseSelectionMode,
                    onToggleSelectAll = onToggleSelectAll,
                    onBatchCopy = onBatchCopy,
                    onBatchCut = onBatchCut,
                    onBatchDelete = onBatchDelete
                )
            } else if (isSearchActive) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCloseSearch,
                        modifier = Modifier.size(Dimensions.ActionButtonSize)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.close_search),
                            tint = AppColors.textPrimary
                        )
                    }
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.weight(1f).focusRequester(searchFocusRequester),
                        singleLine = true,
                        label = { Text(stringResource(R.string.search_this_location)) },
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
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(Dimensions.ActionButtonSize)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_up),
                            tint = AppColors.textPrimary
                        )
                    }
                    Text(
                        text = breadcrumbs.lastOrNull()?.title.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onOpenSearch, modifier = Modifier.size(Dimensions.ActionButtonSize)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = stringResource(R.string.search_files),
                            tint = AppColors.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(Dimensions.ActionButtonSize)
                            .toggleable(value = isDarkTheme, role = Role.Switch) { onToggleTheme() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(if (isDarkTheme) R.drawable.ic_mode_light else R.drawable.ic_mode_dark),
                            contentDescription = stringResource(R.string.theme_toggle),
                            tint = AppColors.textSecondary,
                            modifier = Modifier.size(Dimensions.IconSizeMedium)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { optionsMenuExpanded = true },
                            modifier = Modifier.size(Dimensions.ActionButtonSize)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = stringResource(R.string.options_menu),
                                tint = AppColors.textPrimary
                            )
                        }
                        ExplorerOptionsMenu(
                            expanded = optionsMenuExpanded,
                            showHiddenFiles = showHiddenFiles,
                            currentSortOption = sortOption,
                            onDismissRequest = { optionsMenuExpanded = false },
                            onCreateFolderClick = onCreateFolderClick,
                            onEnterSelectionMode = onEnterSelectionMode,
                            onToggleHiddenFiles = onToggleHiddenFiles,
                            onSortOptionSelected = onSortOptionSelected,
                            onRefreshClick = onRefreshClick
                        )
                    }
                }
            }

            if (parentBreadcrumbs.isNotEmpty() && !isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(start = Dimensions.ActionButtonSize, end = Dimensions.SpacingSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    parentBreadcrumbs.forEachIndexed { index, crumb ->
                        if (index > 0) {
                            Text(
                                text = "›",
                                color = AppColors.textMuted,
                                modifier = Modifier.padding(horizontal = Dimensions.SpacingExtraSmall)
                            )
                        }
                        Text(
                            text = crumb.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.textSecondary,
                            maxLines = 1,
                            modifier = Modifier
                                .heightIn(min = Dimensions.ActionButtonSize)
                                .wrapContentHeight(Alignment.CenterVertically)
                                .clickable(role = Role.Button) { onBreadcrumbClick(crumb.path) }
                                .padding(horizontal = Dimensions.SpacingExtraSmall)
                        )
                    }
                }
            }
        }
    }
}
