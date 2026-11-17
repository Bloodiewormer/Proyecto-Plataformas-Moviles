package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.NoteContent
import cr.ac.una.glifo.domain.model.SyllabusTopic
import cr.ac.una.glifo.domain.repository.CourseContextRepository
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Contenido del apunte + los términos del glosario del curso guardados en el teléfono (#27). */
data class NoteContentUiData(
    val content: NoteContent,
    val glossaryTerms: List<String>,
    val isReconstructing: Boolean = false,
    val reconstructError: AppError? = null,
    val courseTopics: List<SyllabusTopic> = emptyList(),
    val isTopicsLoading: Boolean = false,
    val isUpdatingTopic: Boolean = false,
    val topicError: AppError? = null
)

/**
 * Shared by the "reading a captured note" cluster — NoteDetail, CorrectFragment,
 * FormulaViewer, RegionProvenance — since all four key off the same [NoteContent] for a
 * given `noteId`. Each screen gets its own instance via `hiltViewModel()` and calls [load].
 */
@HiltViewModel
class NoteContentViewModel : ViewModel {

    private val noteRepository: NoteRepository
    private val courseContextRepository: CourseContextRepository
    private val courseRepository: CourseRepository?

    @Inject
    constructor(
        noteRepository: NoteRepository,
        courseContextRepository: CourseContextRepository,
        courseRepository: CourseRepository
    ) : super() {
        this.noteRepository = noteRepository
        this.courseContextRepository = courseContextRepository
        this.courseRepository = courseRepository
    }

    constructor(
        noteRepository: NoteRepository,
        courseContextRepository: CourseContextRepository
    ) : super() {
        this.noteRepository = noteRepository
        this.courseContextRepository = courseContextRepository
        this.courseRepository = null
    }

    private val _state = MutableStateFlow<UiState<NoteContentUiData>>(UiState.Loading)
    val state: StateFlow<UiState<NoteContentUiData>> = _state.asStateFlow()

    private val _saveError = MutableStateFlow<AppError?>(null)
    val saveError: StateFlow<AppError?> = _saveError.asStateFlow()

    private var loadedFor: Long? = null
    private var topicsLoadedForCourseId: Long? = null
    private var observeJob: Job? = null
    private var topicsJob: Job? = null

    fun load(noteId: Long, force: Boolean = false) {
        if (!force && loadedFor == noteId) return
        loadedFor = noteId
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            if (!force) _state.value = UiState.Loading
            applyResult(noteRepository.getNoteContent(noteId), clearReconstructError = force)
            noteRepository.observeNoteContent(noteId).collect { result ->
                applyResult(result, clearReconstructError = false)
            }
        }
    }

    private suspend fun applyResult(result: Result<NoteContent>, clearReconstructError: Boolean) {
        val current = (_state.value as? UiState.Success)?.data
        _state.value = when (result) {
            is Result.Success -> {
                val glossary = courseContextRepository.getCached(result.value.note.courseId)?.glossaryTerms
                    ?: current?.glossaryTerms.orEmpty()
                val sameCourse = current?.content?.note?.courseId == result.value.note.courseId
                UiState.Success(
                    NoteContentUiData(
                        content = result.value,
                        glossaryTerms = glossary,
                        isReconstructing = current?.isReconstructing ?: false,
                        reconstructError = if (clearReconstructError || !result.value.canReconstruct) {
                            null
                        } else {
                            current?.reconstructError
                        },
                        courseTopics = if (sameCourse) current.courseTopics else emptyList(),
                        isTopicsLoading = if (sameCourse) current.isTopicsLoading else false,
                        isUpdatingTopic = current?.isUpdatingTopic ?: false,
                        topicError = current?.topicError
                    )
                )
            }
            is Result.Failure -> UiState.Error(result.error)
        }
        if (result is Result.Success) {
            val courseId = result.value.note.courseId
            if (topicsLoadedForCourseId != courseId) {
                loadCourseTopics(courseId)
            }
        }
    }

    fun loadCourseTopics(courseId: Long, force: Boolean = false) {
        val repo = courseRepository ?: return
        if (!force && topicsLoadedForCourseId == courseId) return
        topicsLoadedForCourseId = courseId
        topicsJob?.cancel()
        topicsJob = viewModelScope.launch {
            val currentBefore = (_state.value as? UiState.Success)?.data
            if (currentBefore != null && currentBefore.content.note.courseId == courseId) {
                _state.value = UiState.Success(currentBefore.copy(isTopicsLoading = true))
            }
            val result = repo.getCourseTopics(courseId)
            val currentAfter = (_state.value as? UiState.Success)?.data ?: return@launch
            if (currentAfter.content.note.courseId != courseId) return@launch
            _state.value = UiState.Success(
                when (result) {
                    is Result.Success -> currentAfter.copy(
                        courseTopics = result.value,
                        isTopicsLoading = false
                    )
                    is Result.Failure -> currentAfter.copy(isTopicsLoading = false)
                }
            )
        }
    }

    fun updateTopic(noteId: Long, topicId: Long?) {
        val current = (_state.value as? UiState.Success)?.data ?: return
        if (current.isUpdatingTopic) return

        viewModelScope.launch {
            _state.value = UiState.Success(current.copy(isUpdatingTopic = true, topicError = null))
            when (val result = noteRepository.updateNoteTopic(noteId, topicId)) {
                is Result.Success -> {
                    val latest = (_state.value as? UiState.Success)?.data ?: current
                    _state.value = UiState.Success(
                        latest.copy(
                            content = latest.content.copy(note = result.value),
                            isUpdatingTopic = false,
                            topicError = null
                        )
                    )
                }
                is Result.Failure -> {
                    val latest = (_state.value as? UiState.Success)?.data ?: current
                    _state.value = UiState.Success(
                        latest.copy(
                            isUpdatingTopic = false,
                            topicError = result.error
                        )
                    )
                }
            }
        }
    }

    fun acceptSuggestedTopic(noteId: Long) {
        val current = (_state.value as? UiState.Success)?.data ?: return
        val suggestedId = current.content.suggestedTopicId ?: return
        if (current.content.note.syllabusTopicId != null) return
        updateTopic(noteId, suggestedId)
    }

    fun clearTopicError() {
        val current = (_state.value as? UiState.Success)?.data ?: return
        if (current.topicError != null) {
            _state.value = UiState.Success(current.copy(topicError = null))
        }
    }

    /**
     * Solicita la reconstrucción IA-01 del apunte (#42). Mientras corre muestra estado de
     * carga ("Reconstruyendo con IA…") y, al terminar, refresca con el documento reconstruido.
     */
    fun reconstructNote(noteId: Long) {
        val current = (_state.value as? UiState.Success)?.data ?: return
        if (current.isReconstructing) return

        viewModelScope.launch {
            _state.value = UiState.Success(
                current.copy(isReconstructing = true, reconstructError = null)
            )
            when (val result = noteRepository.reconstructNote(noteId)) {
                is Result.Success -> {
                    val glossary = courseContextRepository.getCached(result.value.note.courseId)?.glossaryTerms
                        ?: current.glossaryTerms
                    _state.value = UiState.Success(
                        current.copy(
                            content = result.value,
                            glossaryTerms = glossary,
                            isReconstructing = false,
                            reconstructError = null
                        )
                    )
                }
                is Result.Failure -> {
                    _state.value = UiState.Success(
                        current.copy(isReconstructing = false, reconstructError = result.error)
                    )
                }
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
