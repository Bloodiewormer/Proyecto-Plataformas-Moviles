package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.domain.model.CourseDetail
import cr.ac.una.glifo.domain.repository.CourseContextRepository
import cr.ac.una.glifo.domain.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** CaptureScreen only needs the course name/code for its top bar label. */
@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val courseContextRepository: CourseContextRepository,
    private val activeCourseStore: ActiveCourseStore
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<CourseDetail>>(UiState.Loading)
    val state: StateFlow<UiState<CourseDetail>> = _state.asStateFlow()

    private var loadedFor: Long? = null

    fun load(courseId: Long) {
        if (loadedFor == courseId) return
        loadedFor = courseId
        activeCourseStore.set(courseId)
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = courseRepository.getCourseDetail(courseId).toUiState()
        }
        // #27: mientras el estudiante encuadra la foto se actualiza el glosario del curso en
        // Room. Si no hay red no pasa nada: el pipeline usa la última copia guardada.
        viewModelScope.launch { courseContextRepository.refresh(courseId) }
    }
}
