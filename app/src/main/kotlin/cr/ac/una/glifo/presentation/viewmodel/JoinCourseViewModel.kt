package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
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

data class JoinCourseResult(val course: Course, val stats: CourseStats)

@HiltViewModel
class JoinCourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val activeCourseStore: ActiveCourseStore
) : ViewModel() {

    /** Null until the user searches once — lets the screen tell "not searched" from "not found". */
    private val _searchState = MutableStateFlow<UiState<JoinCourseResult>?>(null)
    val searchState: StateFlow<UiState<JoinCourseResult>?> = _searchState.asStateFlow()

    private val _joinState = MutableStateFlow<UiState<Unit>?>(null)
    val joinState: StateFlow<UiState<Unit>?> = _joinState.asStateFlow()

    fun search(joinCode: String) {
        viewModelScope.launch {
            _searchState.value = UiState.Loading
            when (val result = courseRepository.findCourseByJoinCode(joinCode)) {
                is Result.Success -> {
                    val statsResult = courseRepository.getCourseStats(result.value.id)
                    val stats = (statsResult as? Result.Success)?.value
                    _searchState.value = if (stats != null) {
                        UiState.Success(JoinCourseResult(result.value, stats))
                    } else {
                        UiState.Error(AppError.Unknown)
                    }
                }
                is Result.Failure -> _searchState.value = UiState.Error(result.error)
            }
        }
    }

    fun joinCourse(joinCode: String) {
        viewModelScope.launch {
            _joinState.value = UiState.Loading
            when (val result = courseRepository.joinCourse(joinCode)) {
                is Result.Success -> {
                    // El curso al que se acaba de unir pasa a ser el activo: inicio, apuntes y estudio lo siguen.
                    (_searchState.value as? UiState.Success)?.data?.course?.let { activeCourseStore.set(it.id, it.name) }
                    _joinState.value = UiState.Success(Unit)
                }
                is Result.Failure -> _joinState.value = UiState.Error(result.error)
            }
        }
    }

    fun reset() {
        _searchState.value = null
        _joinState.value = null
    }
}
