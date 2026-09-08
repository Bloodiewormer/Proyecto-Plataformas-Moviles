package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.model.NoteContent
import cr.ac.una.glifo.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shared by the "reading a captured note" cluster — NoteDetail, CorrectFragment,
 * FormulaViewer, RegionProvenance — since all four key off the same [NoteContent] for a
 * given `noteId` (mirrors [cr.ac.una.glifo.domain.model.NoteContent]'s own doc comment).
 * Each screen gets its own instance via `hiltViewModel()` and calls [load] with its noteId.
 */
@HiltViewModel
class NoteContentViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<NoteContent>>(UiState.Loading)
    val state: StateFlow<UiState<NoteContent>> = _state.asStateFlow()

    private var loadedFor: Long? = null

    fun load(noteId: Long) {
        if (loadedFor == noteId) return
        loadedFor = noteId
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = noteRepository.getNoteContent(noteId).toUiState()
        }
    }

    fun saveCorrection(noteId: Long, regionIndex: Int, correctedText: String) {
        viewModelScope.launch {
            noteRepository.saveCorrection(noteId, regionIndex, correctedText)
        }
    }
}
