package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.datastore.AppPreferences
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CaptureQualityCheck
import cr.ac.una.glifo.domain.model.CaptureQualityIssue
import cr.ac.una.glifo.domain.repository.CaptureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Normalizer
import javax.inject.Inject

enum class CaptureQualityWarning {
    Blurry,
    Overexposed,
    Underexposed,
    Multiple,
    Generic
}

data class CaptureReviewUiData(
    val checks: List<CaptureQualityCheck>,
    val hasWarning: Boolean,
    val warning: CaptureQualityWarning? = selectQualityWarning(checks, hasWarning)
)

internal fun selectQualityWarning(
    checks: List<CaptureQualityCheck>,
    hasWarning: Boolean
): CaptureQualityWarning? {
    if (!hasWarning) return null
    val failing = checks.filter { it.isWarning }
    if (failing.isEmpty()) return CaptureQualityWarning.Generic

    val distinctIssues = failing.map(::classifyQualityIssue).distinct()
    return when {
        distinctIssues.size > 1 -> CaptureQualityWarning.Multiple
        distinctIssues.single() == CaptureQualityIssue.Blurry -> CaptureQualityWarning.Blurry
        distinctIssues.single() == CaptureQualityIssue.Overexposed -> CaptureQualityWarning.Overexposed
        distinctIssues.single() == CaptureQualityIssue.Underexposed -> CaptureQualityWarning.Underexposed
        else -> CaptureQualityWarning.Generic
    }
}

private fun classifyQualityIssue(check: CaptureQualityCheck): CaptureQualityIssue {
    check.issue?.let { return it }
    val score = normalizeQualityText(check.scoreLabel)
    val label = normalizeQualityText(check.label)
    return when {
        score.contains("sobreexp") ||
            score.contains("overexpos") ||
            score.contains("exceso de luz") ||
            score.contains("too bright") ||
            score.contains("glare") -> CaptureQualityIssue.Overexposed
        score.contains("subexp") ||
            score.contains("underexpos") ||
            score.contains("oscuro") ||
            score.contains("oscura") ||
            score.contains("too dark") ||
            score.contains("dark") ||
            score.contains("falta iluminac") ||
            score.contains("low light") -> CaptureQualityIssue.Underexposed
        score.contains("borros") ||
            score.contains("blur") ||
            score.contains("desenfoc") ||
            score.contains("out of focus") -> CaptureQualityIssue.Blurry
        label.contains("enfoque") ||
            label.contains("nitidez") ||
            label.contains("focus") ||
            label.contains("sharp") ||
            label.contains("blur") -> CaptureQualityIssue.Blurry
        else -> CaptureQualityIssue.Other
    }
}

private fun normalizeQualityText(raw: String): String {
    val decomposed = Normalizer.normalize(raw.lowercase(), Normalizer.Form.NFD)
    return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
}

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
                    val warning = (captureRepository.hasQualityWarning(photoPath) as? Result.Success)?.value
                        ?: checksResult.value.any { it.isWarning }
                    _state.value = UiState.Success(CaptureReviewUiData(checksResult.value, warning))
                }
                is Result.Failure -> _state.value = UiState.Error(checksResult.error)
            }
        }
    }
}
