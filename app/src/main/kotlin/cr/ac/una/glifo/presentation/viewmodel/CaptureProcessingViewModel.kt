package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.model.CaptureStep
import cr.ac.una.glifo.domain.repository.CaptureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaptureProcessingViewModel @Inject constructor(
    private val captureRepository: CaptureRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<CaptureStep>>>(UiState.Loading)
    val state: StateFlow<UiState<List<CaptureStep>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = captureRepository.getProcessingSteps().toUiStateOrEmpty()
        }
    }
}
