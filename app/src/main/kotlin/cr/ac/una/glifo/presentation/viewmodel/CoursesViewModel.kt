package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.Course
import cr.ac.una.glifo.domain.model.CourseStats
import cr.ac.una.glifo.domain.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CourseWithStats(val course: Course, val stats: CourseStats)

/** CoursesScreen (student "Mis cursos"). */
@HiltViewModel
class CoursesViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<CourseWithStats>>>(UiState.Loading)
    val state: StateFlow<UiState<List<CourseWithStats>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = courseRepository.getStudentCourses()) {
                is Result.Success -> _state.value = UiState.Success(withStats(result.value))
                is Result.Failure -> _state.value = UiState.Error(result.error)
            }
        }
    }

    private suspend fun withStats(courses: List<Course>): List<CourseWithStats> =
        courses.mapNotNull { course ->
            val statsResult = courseRepository.getCourseStats(course.id)
            (statsResult as? Result.Success)?.value?.let { CourseWithStats(course, it) }
        }
}
