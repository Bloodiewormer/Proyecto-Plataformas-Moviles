package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.datastore.AppPreferences
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CaptureQualityCheck
import cr.ac.una.glifo.domain.repository.CaptureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CaptureReviewUiData(val checks: List<CaptureQualityCheck>, val hasWarning: Boolean)

@HiltViewModel
class CaptureReviewViewModel @Inject constructor(
    private val captureRepository: CaptureRepository,
    private val appPreferences: AppPreferences? = null
) : ViewModel() {

    /** Ajustes → "Rechazar fotos de baja calidad": con aviso de calidad no se deja procesar igual. */
    val rejectLowQuality: StateFlow<Boolean> = appPreferences?.rejectLowQualityFlow
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
        ?: MutableStateFlow(false).asStateFlow()

    private val _state = MutableStateFlow<UiState<CaptureReviewUiData>>(UiState.Loading)
    val state: StateFlow<UiState<CaptureReviewUiData>> = _state.asStateFlow()

    init { load() }

    fun load(photoPath: String? = null) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val checksResult = captureRepository.getQualityChecks(photoPath)) {
                is Result.Success -> {
                    val warning = (captureRepository.hasQualityWarning(photoPath) as? Result.Success)?.value ?: false
                    _state.value = UiState.Success(CaptureReviewUiData(checksResult.value, warning))
                }
                is Result.Failure -> _state.value = UiState.Error(checksResult.error)
            }
        }
    }
}
