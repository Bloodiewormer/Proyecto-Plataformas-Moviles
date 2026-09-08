package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.GlossaryEntry
import cr.ac.una.glifo.domain.model.GlossarySuggestion
import cr.ac.una.glifo.domain.repository.TeacherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeacherGlossaryUiData(
    val entries: List<GlossaryEntry>,
    val suggestions: List<GlossarySuggestion>
)

@HiltViewModel
class TeacherGlossaryViewModel @Inject constructor(
    private val teacherRepository: TeacherRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<TeacherGlossaryUiData>>(UiState.Loading)
    val state: StateFlow<UiState<TeacherGlossaryUiData>> = _state.asStateFlow()

    init { load() }

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

    fun addEntry(term: String, notation: String, isSymbol: Boolean) {
        viewModelScope.launch {
            teacherRepository.addGlossaryEntry(term, notation, isSymbol)
            load()
        }
    }
}
