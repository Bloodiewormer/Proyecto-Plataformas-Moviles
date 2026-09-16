package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CaptureStep
import cr.ac.una.glifo.domain.model.PageAnalysis
import cr.ac.una.glifo.domain.repository.CaptureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * [analysis] es null cuando se entra a la pantalla sin foto (por ejemplo desde la navegacion
 * de demo): en ese caso solo hay linea de tiempo, sin regiones reales que mostrar.
 */
data class CaptureProcessingUiData(
    val steps: List<CaptureStep>,
    val analysis: PageAnalysis?
)

@HiltViewModel
class CaptureProcessingViewModel @Inject constructor(
    private val captureRepository: CaptureRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<CaptureProcessingUiData>>(UiState.Loading)
    val state: StateFlow<UiState<CaptureProcessingUiData>> = _state.asStateFlow()

    private var loadedFor: String? = null
    private var hasLoaded = false

    /**
     * Corre el pipeline local sobre la foto. Se ignora la recarga si ya se analizo esa misma
     * foto con exito, porque N0+N1 sobre una pagina completa cuesta segundos y la pantalla
     * se recompone varias veces.
     */
    fun load(photoPath: String?) {
        if (hasLoaded && loadedFor == photoPath && _state.value is UiState.Success) return
        loadedFor = photoPath
        hasLoaded = true

        viewModelScope.launch {
            _state.value = UiState.Loading

            // El analisis se pide primero para que el repositorio lo cachee y los pasos se
            // deriven de ese mismo resultado en lugar de volver a procesar la imagen.
            val analysis = photoPath?.let { captureRepository.analyzePage(it) }

            when (val steps = captureRepository.getProcessingSteps(photoPath)) {
                is Result.Success -> _state.value = UiState.Success(
                    CaptureProcessingUiData(
                        steps = steps.value,
                        analysis = (analysis as? Result.Success)?.value
                    )
                )
                is Result.Failure -> _state.value = UiState.Error(steps.error)
            }
        }
    }

    fun retry() = load(loadedFor)
}
