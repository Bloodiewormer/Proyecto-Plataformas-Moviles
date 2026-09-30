package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.NoteMeta
import cr.ac.una.glifo.domain.repository.NoteRepository
import cr.ac.una.glifo.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesUiData(val notes: List<NoteMeta>, val pendingSyncCount: Int)

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<NotesUiData>>(UiState.Loading)
    val state: StateFlow<UiState<NotesUiData>> = _state.asStateFlow()

    private var courseId: Long? = null
    private var loadJob: Job? = null

    /** Apuntes del curso: del servidor si hay red, de Room si no (#25). */
    fun load(courseId: Long) {
        this.courseId = courseId
        // Al cambiar de curso se cancela la carga anterior: si terminara después, pisaría la del nuevo.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = noteRepository.getNoteMetaList(courseId)) {
                is Result.Success -> {
                    val pending = (syncRepository.getPendingCount() as? Result.Success)?.value ?: 0
                    _state.value = UiState.Success(NotesUiData(result.value, pending))
                }
                is Result.Failure -> _state.value = UiState.Error(result.error)
            }
        }
    }

    fun load() {
        courseId?.let { load(it) }
    }
}
