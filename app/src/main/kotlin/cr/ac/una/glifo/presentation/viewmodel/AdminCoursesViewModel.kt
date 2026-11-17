package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.Course
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.TeacherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Screen J4: Cursos (vista de Admin) — every course in the system, not just "sus" cursos. */
@HiltViewModel
class AdminCoursesViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val teacherRepository: TeacherRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<CourseWithStats>>>(UiState.Loading)
    val state: StateFlow<UiState<List<CourseWithStats>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val studentCoursesResult = courseRepository.getStudentCourses()
            val teacherCoursesResult = courseRepository.getTeacherCourses()
            if (studentCoursesResult is Result.Failure && teacherCoursesResult is Result.Failure) {
                _state.value = UiState.Error(studentCoursesResult.error)
                return@launch
            }
            val courses: List<Course> = (
                (studentCoursesResult as? Result.Success)?.value.orEmpty() +
                    (teacherCoursesResult as? Result.Success)?.value.orEmpty()
                ).distinctBy { it.id }
            _state.value = UiState.Success(withStats(courses))
        }
    }

    private suspend fun withStats(courses: List<Course>): List<CourseWithStats> =
        courses.mapNotNull { course ->
            val stats = (courseRepository.getCourseStats(course.id) as? Result.Success)?.value
                ?: return@mapNotNull null
            val group = (teacherRepository.getGroupCoverage(course.id) as? Result.Success)?.value
            CourseWithStats(course, group?.let { stats.withGroup(it) } ?: stats)
        }
}
