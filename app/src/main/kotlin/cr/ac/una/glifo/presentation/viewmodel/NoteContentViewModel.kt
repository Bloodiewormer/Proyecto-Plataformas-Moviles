package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.NoteContent
import cr.ac.una.glifo.domain.repository.CourseContextRepository
import cr.ac.una.glifo.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Contenido del apunte + los términos del glosario del curso guardados en el teléfono (#27). */
data class NoteContentUiData(
    val content: NoteContent,
    val glossaryTerms: List<String>
)

/**
 * Shared by the "reading a captured note" cluster — NoteDetail, CorrectFragment,
 * FormulaViewer, RegionProvenance — since all four key off the same [NoteContent] for a
 * given `noteId`. Each screen gets its own instance via `hiltViewModel()` and calls [load].
 */
@HiltViewModel
class NoteContentViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val courseContextRepository: CourseContextRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<NoteContentUiData>>(UiState.Loading)
    val state: StateFlow<UiState<NoteContentUiData>> = _state.asStateFlow()

    private val _saveError = MutableStateFlow<AppError?>(null)
    val saveError: StateFlow<AppError?> = _saveError.asStateFlow()

    private var loadedFor: Long? = null

    fun load(noteId: Long, force: Boolean = false) {
        if (!force && loadedFor == noteId) return
        loadedFor = noteId
        viewModelScope.launch {
            if (!force) _state.value = UiState.Loading
            _state.value = when (val result = noteRepository.getNoteContent(noteId)) {
                is Result.Success -> {
                    val glossary = courseContextRepository.getCached(result.value.note.courseId)?.glossaryTerms.orEmpty()
                    UiState.Success(NoteContentUiData(result.value, glossary))
                }
                is Result.Failure -> UiState.Error(result.error)
            }
        }
    }

    /** Guarda la corrección y, si salió bien, avisa con [onSaved] para cerrar la pantalla. */
    fun saveCorrection(noteId: Long, fragmentId: String, correctedText: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            when (val result = noteRepository.saveCorrection(noteId, fragmentId, correctedText)) {
                is Result.Success -> {
                    _saveError.value = null
                    load(noteId, force = true)
                    onSaved()
                }
                is Result.Failure -> _saveError.value = result.error
            }
        }
    }
}
