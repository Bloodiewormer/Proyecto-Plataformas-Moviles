package cr.ac.una.glifo.presentation.viewmodel

import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CourseDetail
import cr.ac.una.glifo.domain.model.CourseStats
import cr.ac.una.glifo.domain.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CourseDetailUiData(val course: CourseDetail?, val stats: CourseStats)

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val activeCourseStore: ActiveCourseStore
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<CourseDetailUiData>>(UiState.Loading)
    val state: StateFlow<UiState<CourseDetailUiData>> = _state.asStateFlow()

    private var loadedFor: Long? = null

    fun load(courseId: Long) {
        if (loadedFor == courseId) return
        loadedFor = courseId
        // Abrir un curso lo vuelve el curso activo del inicio y del menú.
        activeCourseStore.set(courseId)
        viewModelScope.launch {
            _state.value = UiState.Loading
            val statsResult = courseRepository.getCourseStats(courseId)
            val stats = (statsResult as? Result.Success)?.value
            if (stats == null) {
                _state.value = UiState.Error((statsResult as Result.Failure).error)
                return@launch
            }
            val courseResult = courseRepository.getCourseDetail(courseId)
            (courseResult as? Result.Success)?.value?.let { activeCourseStore.set(it.id, it.name) }
            _state.value = UiState.Success(
                CourseDetailUiData(course = (courseResult as? Result.Success)?.value, stats = stats)
            )
        }
    }
}
