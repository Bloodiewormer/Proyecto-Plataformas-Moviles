package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CourseStats
import cr.ac.una.glifo.domain.model.TeacherGapTopic
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.TeacherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TEACHER_ACTIVE_COURSE_ID = 3L

data class TeacherGapsUiData(
    val stats: CourseStats,
    val gaps: List<TeacherGapTopic>
)

@HiltViewModel
class TeacherGapsViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val teacherRepository: TeacherRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<TeacherGapsUiData>>(UiState.Loading)
    val state: StateFlow<UiState<TeacherGapsUiData>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val statsResult = courseRepository.getCourseStats(TEACHER_ACTIVE_COURSE_ID)) {
                is Result.Failure -> _state.value = UiState.Error(statsResult.error)
                is Result.Success -> {
                    when (val gapsResult = teacherRepository.getGaps()) {
                        is Result.Failure -> _state.value = UiState.Error(gapsResult.error)
                        is Result.Success -> _state.value = UiState.Success(
                            TeacherGapsUiData(statsResult.value, gapsResult.value)
                        )
                    }
                }
            }
        }
    }
}
