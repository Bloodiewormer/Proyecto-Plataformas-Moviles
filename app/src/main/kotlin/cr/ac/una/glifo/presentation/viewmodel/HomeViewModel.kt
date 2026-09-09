package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CourseDetail
import cr.ac.una.glifo.domain.model.CourseStats
import cr.ac.una.glifo.domain.model.NoteMeta
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.NoteRepository
import cr.ac.una.glifo.domain.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiData(
    val course: CourseDetail?,
    val stats: CourseStats,
    val recentNotes: List<NoteMeta>,
    val pendingSyncCount: Int
)

/** Screen B2: Inicio Estudiante. Hardcodes the same "active course" the dummy data assumed. */
class HomeViewModel(
    private val courseRepository: CourseRepository,
    private val noteRepository: NoteRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    companion object {
        const val ACTIVE_COURSE_ID = 1L
    }

    private val _state = MutableStateFlow<UiState<HomeUiData>>(UiState.Loading)
    val state: StateFlow<UiState<HomeUiData>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val statsResult = courseRepository.getCourseStats(ACTIVE_COURSE_ID)
            val stats = (statsResult as? Result.Success)?.value
            if (stats == null) {
                _state.value = UiState.Error((statsResult as Result.Failure).error)
                return@launch
            }
            val courseResult = courseRepository.getCourseDetail(ACTIVE_COURSE_ID)
            val notesResult = noteRepository.getNoteMetaList()
            val pendingResult = syncRepository.getPendingCount()

            _state.value = UiState.Success(
                HomeUiData(
                    course = (courseResult as? Result.Success)?.value,
                    stats = stats,
                    recentNotes = (notesResult as? Result.Success)?.value.orEmpty().take(2),
                    pendingSyncCount = (pendingResult as? Result.Success)?.value ?: 0
                )
            )
        }
    }
}
