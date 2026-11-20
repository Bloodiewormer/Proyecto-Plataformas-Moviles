package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
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
    private val teacherRepository: TeacherRepository,
    private val activeCourseStore: ActiveCourseStore
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<TeacherHomeUiData>>(UiState.Loading)
    val state: StateFlow<UiState<TeacherHomeUiData>> = _state.asStateFlow()

    init {
        viewModelScope.launch { activeCourseStore.activeCourseId.drop(1).distinctUntilChanged().collect { load() } }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val courseId = activeCourseStore.resolveTeacherCourseId(courseRepository)
            if (courseId == null) {
                _state.value = UiState.Empty
                return@launch
            }
            val statsResult = courseRepository.getCourseStats(courseId)
            val stats = (statsResult as? Result.Success)?.value
            if (stats == null) {
                _state.value = UiState.Error((statsResult as Result.Failure).error)
                return@launch
            }
            val courseResult = courseRepository.getCourseDetail(courseId)
            // getCourseStats es la cobertura del propio docente; la del grupo sale de coverage/gaps.
            val group = (teacherRepository.getGroupCoverage() as? Result.Success)?.value

            _state.value = UiState.Success(
                TeacherHomeUiData(
                    course = (courseResult as? Result.Success)?.value,
                    stats = group?.let { stats.withGroup(it) } ?: stats,
                    gaps = group?.topics.orEmpty()
                )
            )
        }
    }
}
