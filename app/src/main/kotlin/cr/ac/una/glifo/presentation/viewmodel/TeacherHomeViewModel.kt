package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CourseDetail
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

data class TeacherHomeUiData(
    val course: CourseDetail?,
    val stats: CourseStats,
    val gaps: List<TeacherGapTopic>
)

/** Screen B3: Inicio Docente. */
@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val teacherRepository: TeacherRepository
) : ViewModel() {

    companion object {
        const val ACTIVE_COURSE_ID = 3L
    }

    private val _state = MutableStateFlow<UiState<TeacherHomeUiData>>(UiState.Loading)
    val state: StateFlow<UiState<TeacherHomeUiData>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val statsResult = courseRepository.getCourseStats(ACTIVE_COURSE_ID)
            val stats = (statsResult as? Result.Success)?.value
            if (stats == null) {
                _state.value = UiState.Error((statsResult as Result.Failure).error)
                return@launch
            }
            val courseResult = courseRepository.getCourseDetail(ACTIVE_COURSE_ID)
            val gapsResult = teacherRepository.getGaps()

            _state.value = UiState.Success(
                TeacherHomeUiData(
                    course = (courseResult as? Result.Success)?.value,
                    stats = stats,
                    gaps = (gapsResult as? Result.Success)?.value.orEmpty()
                )
            )
        }
    }
}
