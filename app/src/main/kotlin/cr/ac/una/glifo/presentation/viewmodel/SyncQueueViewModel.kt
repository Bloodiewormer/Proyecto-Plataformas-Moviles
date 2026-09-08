package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.SyncQueueItem
import cr.ac.una.glifo.domain.model.SyncStatus
import cr.ac.una.glifo.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncQueueUiData(val items: List<SyncQueueItem>, val syncingCount: Int)

@HiltViewModel
class SyncQueueViewModel @Inject constructor(
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<SyncQueueUiData>>(UiState.Loading)
    val state: StateFlow<UiState<SyncQueueUiData>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = syncRepository.getQueueItems()) {
                is Result.Success -> _state.value = UiState.Success(
                    SyncQueueUiData(result.value, result.value.count { it.status == SyncStatus.Syncing })
                )
                is Result.Failure -> _state.value = UiState.Error(result.error)
            }
        }
    }
}
