package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
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

data class TeacherGapsUiData(
    val stats: CourseStats,
    val gaps: List<TeacherGapTopic>
)

@HiltViewModel
class TeacherGapsViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val teacherRepository: TeacherRepository,
    private val activeCourseStore: ActiveCourseStore
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<TeacherGapsUiData>>(UiState.Loading)
    val state: StateFlow<UiState<TeacherGapsUiData>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val courseId = activeCourseStore.resolveTeacherCourseId(courseRepository)
            if (courseId == null) {
                _state.value = UiState.Empty
                return@launch
            }
            when (val statsResult = courseRepository.getCourseStats(courseId)) {
                is Result.Failure -> _state.value = UiState.Error(statsResult.error)
                is Result.Success -> {
                    when (val groupResult = teacherRepository.getGroupCoverage()) {
                        is Result.Failure -> _state.value = UiState.Error(groupResult.error)
                        is Result.Success -> _state.value = UiState.Success(
                            TeacherGapsUiData(statsResult.value.withGroup(groupResult.value), groupResult.value.topics)
                        )
                    }
                }
            }
        }
    }
}

/** Reemplaza en las estadísticas del curso lo que es del grupo (inscritos, % y barra). */
internal fun cr.ac.una.glifo.domain.model.CourseStats.withGroup(
    group: cr.ac.una.glifo.domain.model.TeacherGroupCoverage
): cr.ac.una.glifo.domain.model.CourseStats = copy(
    coveragePercent = group.coveragePercent,
    studentCount = group.enrolledStudents,
    verified = group.solidShare,
    repaired = group.partialShare,
    escalated = group.absentShare,
    uncertain = group.uncertainShare
)
