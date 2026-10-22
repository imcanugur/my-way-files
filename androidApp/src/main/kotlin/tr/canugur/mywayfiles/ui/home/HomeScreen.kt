package tr.canugur.mywayfiles.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
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
import tr.canugur.mywayfiles.ui.home.components.EmptyDrivesView
import tr.canugur.mywayfiles.ui.home.components.HomeTopBar
import tr.canugur.mywayfiles.ui.home.components.OverallStorageCard
import tr.canugur.mywayfiles.ui.home.components.RecycleBinCard
import tr.canugur.mywayfiles.ui.home.components.StorageDriveCard
import tr.canugur.mywayfiles.ui.theme.AppColors
import tr.canugur.mywayfiles.ui.theme.Dimensions

/**
 * Stateful root decoupling ViewModel from stateless UI presentation.
 */
@Composable
fun HomeScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onDriveClick: (StorageDrive) -> Unit,
    onRecycleBinClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(context)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenContent(
        state = state,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme,
        onDriveClick = onDriveClick,
        onRecycleBinClick = onRecycleBinClick,
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
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onDriveClick: (StorageDrive) -> Unit,
    onRecycleBinClick: () -> Unit,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        containerColor = AppColors.canvasBackground,
        topBar = {
            HomeTopBar(
                isDarkTheme = isDarkTheme,
                isRefreshing = (state as? HomeUiState.Success)?.isRefreshing == true,
                onToggleTheme = onToggleTheme,
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
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
