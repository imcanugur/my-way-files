package tr.canugur.mywayfiles.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tr.canugur.mywayfiles.data.datasource.AndroidStorageLocalDataSource
import tr.canugur.mywayfiles.data.repository.StorageRepositoryImpl
import tr.canugur.mywayfiles.domain.model.StorageSummary
import tr.canugur.mywayfiles.domain.usecase.GetStorageDrivesUseCase

class HomeViewModel(
    private val getStorageDrivesUseCase: GetStorageDrivesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDrives()
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.Refresh -> refresh()
            is HomeUiEvent.DriveClick -> {
                // Reserved for directory navigation in future task
            }
        }
    }

    private fun loadDrives() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            fetchDrivesInternal()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is HomeUiState.Success) {
                _uiState.value = currentState.copy(isRefreshing = true)
            }
            fetchDrivesInternal()
        }
    }

    private suspend fun fetchDrivesInternal() {
        try {
            val drives = getStorageDrivesUseCase()
            if (drives.isEmpty()) {
                _uiState.value = HomeUiState.Empty
            } else {
                _uiState.value = HomeUiState.Success(
                    summary = StorageSummary.from(drives),
                    drives = drives,
                    isRefreshing = false
                )
            }
        } catch (e: Exception) {
            _uiState.value = HomeUiState.Error(e.message ?: "Failed to read storage drives")
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val dataSource = AndroidStorageLocalDataSource(appContext)
                    val repository = StorageRepositoryImpl(dataSource)
                    val useCase = GetStorageDrivesUseCase(repository)
                    return HomeViewModel(useCase) as T
                }
            }
    }
}
