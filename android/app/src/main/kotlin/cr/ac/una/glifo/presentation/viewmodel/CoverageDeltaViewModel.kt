package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.model.CoverageDeltaInfo
import cr.ac.una.glifo.domain.repository.CoverageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoverageDeltaViewModel @Inject constructor(
    private val coverageRepository: CoverageRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<CoverageDeltaInfo>>(UiState.Loading)
    val state: StateFlow<UiState<CoverageDeltaInfo>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = coverageRepository.getDelta().toUiState()
        }
    }
}
