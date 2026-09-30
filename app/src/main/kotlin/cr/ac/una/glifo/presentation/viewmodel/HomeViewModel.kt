package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CourseDetail
import cr.ac.una.glifo.domain.model.CourseStats
import cr.ac.una.glifo.domain.model.NoteMeta
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.NoteRepository
import cr.ac.una.glifo.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiData(
    val course: CourseDetail?,
    /** Métricas de cobertura y estudio; null mientras esos módulos (Lab 5/6) no respondan. */
    val stats: CourseStats?,
    val recentNotes: List<NoteMeta>,
    val pendingSyncCount: Int
)

/**
 * Screen B2: Inicio Estudiante. El curso activo es el último que abrió el estudiante o, la
 * primera vez, el primero de sus cursos; ya no hay un id fijo.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val noteRepository: NoteRepository,
    private val syncRepository: SyncRepository,
    private val activeCourseStore: ActiveCourseStore
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<HomeUiData>>(UiState.Loading)
    val state: StateFlow<UiState<HomeUiData>> = _state.asStateFlow()

    init {
        // Se recarga sola cuando el usuario elige otro curso en el selector.
        viewModelScope.launch { activeCourseStore.activeCourseId.drop(1).distinctUntilChanged().collect { load() } }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val pending = (syncRepository.getPendingCount() as? Result.Success)?.value ?: 0

            val courseId = activeCourseStore.activeCourseId.value ?: when (val courses = courseRepository.getStudentCourses()) {
                is Result.Success -> courses.value.firstOrNull()?.also { activeCourseStore.set(it.id, it.name) }?.id
                is Result.Failure -> {
                    // Sin red y sin curso guardado todavía no hay nada que mostrar del curso,
                    // pero la cola offline sí es real.
                    _state.value = UiState.Success(HomeUiData(null, null, emptyList(), pending))
                    return@launch
                }
            }

            if (courseId == null) {
                _state.value = UiState.Success(HomeUiData(null, null, emptyList(), pending))
                return@launch
            }

            _state.value = UiState.Success(
                HomeUiData(
                    course = (courseRepository.getCourseDetail(courseId) as? Result.Success)?.value
                        ?.also { activeCourseStore.set(it.id, it.name) },
                    stats = (courseRepository.getCourseStats(courseId) as? Result.Success)?.value,
                    recentNotes = (noteRepository.getNoteMetaList(courseId) as? Result.Success)?.value.orEmpty().take(2),
                    pendingSyncCount = pending
                )
            )
        }
    }
}
