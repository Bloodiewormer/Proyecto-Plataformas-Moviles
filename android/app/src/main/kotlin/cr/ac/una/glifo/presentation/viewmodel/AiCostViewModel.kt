package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.model.AiCostStats
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.domain.repository.AiCostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiCostViewModel @Inject constructor(
    private val aiCostRepository: AiCostRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<AiCostStats>>(UiState.Loading)
    val state: StateFlow<UiState<AiCostStats>> = _state.asStateFlow()

    private var loadedFor: GlifoRole? = null

    fun load(role: GlifoRole) {
        if (loadedFor == role) return
        loadedFor = role
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = aiCostRepository.getStats(role).toUiState()
        }
    }
}
