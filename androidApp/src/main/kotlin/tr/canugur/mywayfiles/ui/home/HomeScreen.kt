package tr.canugur.mywayfiles.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.StorageDrive
import tr.canugur.mywayfiles.ui.common.RefreshErrorBanner
import tr.canugur.mywayfiles.ui.common.RetryErrorState
import tr.canugur.mywayfiles.ui.common.AppModalBottomSheet
import tr.canugur.mywayfiles.ui.home.components.EmptyDrivesView
import tr.canugur.mywayfiles.ui.home.components.HomeTopBar
import tr.canugur.mywayfiles.ui.home.components.OverallStorageCard
import tr.canugur.mywayfiles.ui.home.components.RecycleBinCard
import tr.canugur.mywayfiles.ui.home.components.StorageDriveCard
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Stateful root decoupling ViewModel from stateless UI presentation.
 */
@Composable
fun HomeScreen(
    onDriveClick: (StorageDrive) -> Unit,
    onSearchDriveClick: (StorageDrive) -> Unit,
    onRecycleBinClick: () -> Unit,
    onOpenDrawer: () -> Unit,
    onDrivesLoaded: (List<StorageDrive>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(context)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenContent(
        state = state,
        onDriveClick = onDriveClick,
        onSearchDriveClick = onSearchDriveClick,
        onRecycleBinClick = onRecycleBinClick,
        onOpenDrawer = onOpenDrawer,
        onDrivesLoaded = onDrivesLoaded,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

/**
 * Stateless UI Presentation driven purely by state and events.
 */
@Composable
fun HomeScreenContent(
    state: HomeUiState,
    onDriveClick: (StorageDrive) -> Unit,
    onSearchDriveClick: (StorageDrive) -> Unit,
    onRecycleBinClick: () -> Unit,
    onOpenDrawer: () -> Unit,
    onDrivesLoaded: (List<StorageDrive>) -> Unit,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSearchLocationPicker by remember { mutableStateOf(false) }
    val availableDrives = (state as? HomeUiState.Success)?.drives.orEmpty()
    androidx.compose.runtime.LaunchedEffect(availableDrives) {
        if (availableDrives.isNotEmpty()) onDrivesLoaded(availableDrives)
    }
    Scaffold(
        containerColor = AppColors.canvasBackground,
        topBar = {
            HomeTopBar(
                isRefreshing = (state as? HomeUiState.Success)?.isRefreshing == true,
                onOpenDrawer = onOpenDrawer,
                onSearchClick = { showSearchLocationPicker = true },
                onRefreshClick = { onEvent(HomeUiEvent.Refresh) }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        when (state) {
            is HomeUiState.Loading -> {
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

            is HomeUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(Dimensions.SpacingLarge),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyDrivesView()
                }
            }

            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(Dimensions.SpacingLarge),
                    contentAlignment = Alignment.Center
                ) {
                    RetryErrorState(
                        message = state.message,
                        onRetry = { onEvent(HomeUiEvent.Refresh) }
                    )
                }
            }

            is HomeUiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { onEvent(HomeUiEvent.Refresh) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = Dimensions.SpacingLarge,
                            vertical = Dimensions.SpacingDefault
                        ),
                    ) {
                    if (state.hasRefreshError) {
                        item(key = "refresh-error") {
                            RefreshErrorBanner(
                                onRetry = { onEvent(HomeUiEvent.Refresh) }
                            )
                        }
                    }

                    item {
                        OverallStorageCard(summary = state.summary)
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Dimensions.SpacingHuge, bottom = Dimensions.SpacingSmall),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.devices_and_drives),
                                style = MaterialTheme.typography.titleMedium,
                                color = AppColors.textPrimary
                            )
                            Text(
                                text = state.drives.size.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.textMuted
                            )
                        }
                    }

                    itemsIndexed(state.drives, key = { _, drive -> drive.id }) { index, drive ->
                        StorageDriveCard(
                            drive = drive,
                            onClick = { onDriveClick(drive) },
                            modifier = Modifier.padding(vertical = Dimensions.SpacingExtraSmall)
                        )
                        if (index < state.drives.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = Dimensions.IconContainerSize + Dimensions.SpacingHuge),
                                color = AppColors.divider
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Dimensions.SpacingHuge, bottom = Dimensions.SpacingSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.shortcuts),
                                style = MaterialTheme.typography.titleMedium,
                                color = AppColors.textPrimary
                            )
                        }
                        RecycleBinCard(onClick = onRecycleBinClick)
                    }

                    item {
                        Spacer(modifier = Modifier.height(Dimensions.SpacingHuge))
                    }
                    }
            }
        }
    }
    }

    if (showSearchLocationPicker) {
        AppModalBottomSheet(onDismissRequest = { showSearchLocationPicker = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.SpacingLarge)
                    .padding(bottom = Dimensions.SpacingLarge),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
            ) {
                Text(stringResource(R.string.search_files), style = MaterialTheme.typography.titleLarge)
                val drives = (state as? HomeUiState.Success)?.drives.orEmpty()
                if (drives.isEmpty()) {
                    Text(stringResource(R.string.no_drives_found))
                } else {
                    Column {
                        Text(stringResource(R.string.search_choose_location))
                        LazyColumn(modifier = Modifier.heightIn(max = Dimensions.SpacingHuge * 8f)) {
                            itemsIndexed(drives, key = { _, drive -> drive.id }) { _, drive ->
                                TextButton(
                                    onClick = {
                                        showSearchLocationPicker = false
                                        onSearchDriveClick(drive)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(drive.name, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showSearchLocationPicker = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        }
    }
}
