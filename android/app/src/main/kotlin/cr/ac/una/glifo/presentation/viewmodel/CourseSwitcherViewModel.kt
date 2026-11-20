package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.Course
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CourseSwitcherUiData(
    val courses: List<Course>,
    val activeCourseId: Long?,
    val role: GlifoRole
)

/** Selector del curso activo: los cursos del usuario según su rol y cuál está elegido. */
@HiltViewModel
class CourseSwitcherViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val sessionRepository: SessionRepository,
    private val activeCourseStore: ActiveCourseStore
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<CourseSwitcherUiData>>(UiState.Loading)
    val state: StateFlow<UiState<CourseSwitcherUiData>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val role = sessionRepository.currentUser.value?.role ?: GlifoRole.Student
            val result = if (role == GlifoRole.Teacher) courseRepository.getTeacherCourses() else courseRepository.getStudentCourses()
            _state.value = when (result) {
                is Result.Success -> if (result.value.isEmpty()) {
                    UiState.Empty
                } else {
                    UiState.Success(CourseSwitcherUiData(result.value, activeCourseStore.activeCourseId.value, role))
                }
                is Result.Failure -> UiState.Error(result.error)
            }
        }
    }

    /** Deja [course] como curso activo; el inicio y el menú se actualizan solos. */
    fun select(course: Course) {
        activeCourseStore.set(course.id, course.name)
    }
}
