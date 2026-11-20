package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.GlossaryEntry
import cr.ac.una.glifo.domain.model.GlossarySuggestion
import cr.ac.una.glifo.domain.repository.TeacherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeacherGlossaryUiData(
    val entries: List<GlossaryEntry>,
    val suggestions: List<GlossarySuggestion>
)

@HiltViewModel
class TeacherGlossaryViewModel @Inject constructor(
    private val teacherRepository: TeacherRepository,
    activeCourseStore: ActiveCourseStore
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<TeacherGlossaryUiData>>(UiState.Loading)
    val state: StateFlow<UiState<TeacherGlossaryUiData>> = _state.asStateFlow()

    /**
     * Error de la última acción (agregar, aprobar, descartar). Va aparte de [state] para no
     * reemplazar la lista: por ejemplo, un término repetido (409) se avisa sin perder la vista.
     */
    private val _actionError = MutableStateFlow<AppError?>(null)
    val actionError: StateFlow<AppError?> = _actionError.asStateFlow()

    init {
        load()
        // Al cambiar de curso activo, el glosario es otro.
        viewModelScope.launch { activeCourseStore.activeCourseId.drop(1).distinctUntilChanged().collect { load() } }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val entriesResult = teacherRepository.getGlossaryEntries()) {
                is Result.Failure -> _state.value = UiState.Error(entriesResult.error)
                is Result.Success -> {
                    val suggestions = (teacherRepository.getGlossarySuggestions() as? Result.Success)?.value.orEmpty()
                    _state.value = UiState.Success(TeacherGlossaryUiData(entriesResult.value, suggestions))
                }
            }
        }
    }

    fun addEntry(term: String, notation: String, isSymbol: Boolean) =
        runAction { teacherRepository.addGlossaryEntry(term, notation, isSymbol) }

    fun approveSuggestion(suggestionId: Long) =
        runAction { teacherRepository.resolveGlossarySuggestion(suggestionId, approve = true) }

    fun discardSuggestion(suggestionId: Long) =
        runAction { teacherRepository.resolveGlossarySuggestion(suggestionId, approve = false) }

    fun dismissActionError() {
        _actionError.value = null
    }

    private fun runAction(action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _actionError.value = null
            when (val result = action()) {
                is Result.Success -> load()
                is Result.Failure -> _actionError.value = result.error
            }
        }
    }
}
