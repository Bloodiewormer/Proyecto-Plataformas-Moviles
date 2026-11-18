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
    private var lastPendingCount: Int? = null

    init {
        viewModelScope.launch {
            syncRepository.observePendingCount().collect { count ->
                val prev = lastPendingCount
                lastPendingCount = count

                val current = _state.value
                if (current is UiState.Success && current.data.pendingSyncCount != count) {
                    _state.value = UiState.Success(current.data.copy(pendingSyncCount = count))
                }

                if (prev != null && count < prev) {
                    load(showLoading = false)
                }
            }
        }
    }

    /** Apuntes del curso: del servidor si hay red, de Room si no (#25). */
    fun load(courseId: Long, showLoading: Boolean = true) {
        this.courseId = courseId
        // Al cambiar de curso se cancela la carga anterior: si terminara después, pisaría la del nuevo.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (showLoading || _state.value !is UiState.Success) {
                _state.value = UiState.Loading
            }
            when (val result = noteRepository.getNoteMetaList(courseId)) {
                is Result.Success -> {
                    val pending = lastPendingCount ?: (syncRepository.getPendingCount() as? Result.Success)?.value ?: 0
                    _state.value = UiState.Success(NotesUiData(result.value, pending))
                }
                is Result.Failure -> _state.value = UiState.Error(result.error)
            }
        }
    }

    fun load() {
        courseId?.let { load(it) }
    }

    fun load(showLoading: Boolean) {
        courseId?.let { load(it, showLoading) }
    }
}
