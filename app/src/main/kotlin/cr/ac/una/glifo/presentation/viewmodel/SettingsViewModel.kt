package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _pendingCount = MutableStateFlow<UiState<Int>>(UiState.Loading)
    val pendingCount: StateFlow<UiState<Int>> = _pendingCount.asStateFlow()

    init {
        viewModelScope.launch {
            _pendingCount.value = syncRepository.getPendingCount().toUiState()
        }
    }
}
