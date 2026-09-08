package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.SyllabusUnitRow
import cr.ac.una.glifo.domain.repository.TeacherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeacherSyllabusUiData(
    val fileName: String,
    val units: List<SyllabusUnitRow>
)

@HiltViewModel
class TeacherSyllabusViewModel @Inject constructor(
    private val teacherRepository: TeacherRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<TeacherSyllabusUiData>>(UiState.Loading)
    val state: StateFlow<UiState<TeacherSyllabusUiData>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val fileNameResult = teacherRepository.getSyllabusFileName()) {
                is Result.Failure -> _state.value = UiState.Error(fileNameResult.error)
                is Result.Success -> {
                    when (val unitsResult = teacherRepository.getSyllabusUnits()) {
                        is Result.Failure -> _state.value = UiState.Error(unitsResult.error)
                        is Result.Success -> _state.value = UiState.Success(
                            TeacherSyllabusUiData(fileNameResult.value, unitsResult.value)
                        )
                    }
                }
            }
        }
    }

    fun addTopic(label: String) {
        viewModelScope.launch {
            teacherRepository.addSyllabusTopic(label)
            load()
        }
    }
}
