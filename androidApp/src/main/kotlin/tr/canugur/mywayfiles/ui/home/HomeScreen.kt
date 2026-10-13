package tr.canugur.mywayfiles.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tr.canugur.mywayfiles.R
import tr.canugur.mywayfiles.domain.model.StorageDrive
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
                    Text(
                        text = state.message,
                        color = AppColors.statusCritical,
                        fontSize = 13.sp
                    )
                }
            }

            is HomeUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        horizontal = Dimensions.SpacingDefault,
                        vertical = Dimensions.SpacingMedium
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SpacingMedium)
                ) {
                    // Overall Storage Device Header
                    item {
                        OverallStorageCard(summary = state.summary)
                    }

                    // Recycle Bin Quick Access Card
                    item {
                        RecycleBinCard(onClick = onRecycleBinClick)
                    }

                    // Section Header: "Devices and drives"
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = Dimensions.SpacingSmall,
                                    bottom = Dimensions.SpacingExtraSmall
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.devices_and_drives),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.textPrimary
                            )
                            Text(
                                text = "(${state.drives.size})",
                                fontSize = 12.sp,
                                color = AppColors.textMuted
                            )
                        }
                    }

                    // Drives List
                    items(state.drives, key = { it.id }) { drive ->
                        StorageDriveCard(
                            drive = drive,
                            onClick = { onDriveClick(drive) }
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
