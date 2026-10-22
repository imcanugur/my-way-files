package tr.canugur.mywayfiles.ui.home

import tr.canugur.mywayfiles.domain.model.StorageDrive
import tr.canugur.mywayfiles.domain.model.StorageSummary

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val summary: StorageSummary,
        val drives: List<StorageDrive>,
        val isRefreshing: Boolean = false,
        val hasRefreshError: Boolean = false
    ) : HomeUiState

    data object Empty : HomeUiState

    data class Error(val message: String) : HomeUiState
}

sealed interface HomeUiEvent {
    data object Refresh : HomeUiEvent
    data class DriveClick(val drive: StorageDrive) : HomeUiEvent
}
