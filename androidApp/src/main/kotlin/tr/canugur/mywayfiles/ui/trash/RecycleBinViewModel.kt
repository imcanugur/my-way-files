package tr.canugur.mywayfiles.ui.trash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tr.canugur.mywayfiles.data.repository.RecycleBinRepositoryImpl
import tr.canugur.mywayfiles.domain.model.TrashedItem
import tr.canugur.mywayfiles.domain.repository.RecycleBinRepository

/**
 * ViewModel managing Recycle Bin state flow, restore operations, and purging.
 */
class RecycleBinViewModel(
    private val repository: RecycleBinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecycleBinUiState>(RecycleBinUiState.Loading)
    val uiState: StateFlow<RecycleBinUiState> = _uiState.asStateFlow()

    init {
        loadItems()
    }

    fun onEvent(event: RecycleBinUiEvent) {
        when (event) {
            is RecycleBinUiEvent.Refresh -> loadItems()
            is RecycleBinUiEvent.Restore -> restore(event.item)
            is RecycleBinUiEvent.DeletePermanently -> deletePermanently(event.item)
            is RecycleBinUiEvent.EmptyTrash -> emptyTrash()
        }
    }

    fun loadItems() {
        viewModelScope.launch {
            _uiState.value = RecycleBinUiState.Loading
            repository.getTrashedItems().fold(
                onSuccess = { items ->
                    if (items.isEmpty()) {
                        _uiState.value = RecycleBinUiState.Empty
                    } else {
                        val total = items.sumOf { it.sizeBytes }
                        _uiState.value = RecycleBinUiState.Success(items = items, totalBytes = total)
                    }
                },
                onFailure = {
                    _uiState.value = RecycleBinUiState.Error(it.localizedMessage ?: "Failed to read Recycle Bin")
                }
            )
        }
    }

    private fun restore(item: TrashedItem) {
        viewModelScope.launch {
            repository.restore(item)
            loadItems()
        }
    }

    private fun deletePermanently(item: TrashedItem) {
        viewModelScope.launch {
            repository.deletePermanently(item)
            loadItems()
        }
    }

    private fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            loadItems()
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val repo = RecycleBinRepositoryImpl.getInstance(context.applicationContext)
                    return RecycleBinViewModel(repo) as T
                }
            }
    }
}
