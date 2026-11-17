package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.Course
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.TeacherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** TeacherCoursesScreen ("Mis cursos" for the docente role). */
@HiltViewModel
class TeacherCoursesViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val activeCourseStore: ActiveCourseStore,
    private val teacherRepository: TeacherRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<CourseWithStats>>>(UiState.Loading)
    val state: StateFlow<UiState<List<CourseWithStats>>> = _state.asStateFlow()

    /** Para marcar en la lista cuál es el curso activo. */
    val activeCourseId: StateFlow<Long?> = activeCourseStore.activeCourseId

    private var loadJob: Job? = null

    init { load() }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = courseRepository.getTeacherCourses()) {
                is Result.Success -> _state.value = UiState.Success(withStats(result.value))
                is Result.Failure -> _state.value = UiState.Error(result.error)
            }
        }
    }

    private suspend fun withStats(courses: List<Course>): List<CourseWithStats> =
        courses.mapNotNull { course ->
            val stats = (courseRepository.getCourseStats(course.id) as? Result.Success)?.value
                ?: return@mapNotNull null
            val group = (teacherRepository.getGroupCoverage(course.id) as? Result.Success)?.value
            CourseWithStats(course, group?.let { stats.withGroup(it) } ?: stats)
        }

    /** El curso que el docente abre pasa a ser el activo: sílabo, glosario y brechas lo siguen. */
    fun select(course: Course) {
        activeCourseStore.set(course.id, course.name)
    }
}
