package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CourseDetail
import cr.ac.una.glifo.domain.model.QuizQuestion
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudyHomeUiData(
    val course: CourseDetail?,
    val flashcardCount: Int,
    val quizQuestion: QuizQuestion?,
    val quizQuestionCount: Int = 0,
    val isGenerating: Boolean = false,
    val generationError: AppError? = null
)

/**
 * StudyScreen (el punto de entrada de "Estudio" — tarjetas de flashcards y quiz del curso,
 * más la acción de generación por lotes con IA-02, #44).
 */
@HiltViewModel
class StudyHomeViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<StudyHomeUiData>>(UiState.Loading)
    val state: StateFlow<UiState<StudyHomeUiData>> = _state.asStateFlow()

    private var loadedFor: Long? = null
    private var loadJob: Job? = null
    private var generateJob: Job? = null

    fun load(courseId: Long, force: Boolean = false) {
        if (!force && loadedFor == courseId && _state.value is UiState.Success) return
        loadedFor = courseId
        // Al cambiar de curso se cancela la carga anterior: si terminara después, pisaría la del nuevo.
        loadJob?.cancel()
        generateJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = UiState.Loading
            val courseResult = courseRepository.getCourseDetail(courseId)
            val flashcardsResult = studyRepository.getFlashcards(courseId)
            val quizQuestionsResult = studyRepository.getQuizQuestions(courseId)

            if (flashcardsResult is Result.Failure && quizQuestionsResult is Result.Failure) {
                _state.value = UiState.Error(flashcardsResult.error)
                return@launch
            }

            val flashcards = (flashcardsResult as? Result.Success)?.value.orEmpty()
            val quizQuestions = (quizQuestionsResult as? Result.Success)?.value.orEmpty()

            _state.value = UiState.Success(
                StudyHomeUiData(
                    course = (courseResult as? Result.Success)?.value,
                    flashcardCount = flashcards.size,
                    quizQuestion = quizQuestions.firstOrNull(),
                    quizQuestionCount = quizQuestions.size
                )
            )
        }
    }

    /**
     * Genera material de estudio para los temas del curso (máximo 10 temas) y refresca
     * las flashcards y preguntas de quiz (#44).
     */
    fun generateStudyMaterial(courseId: Long) {
        val current = (_state.value as? UiState.Success)?.data
        if (current?.isGenerating == true) return

        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            val base = current ?: StudyHomeUiData(
                course = null,
                flashcardCount = 0,
                quizQuestion = null,
                quizQuestionCount = 0
            )
            _state.value = UiState.Success(base.copy(isGenerating = true, generationError = null))

            val topicsResult = courseRepository.getCourseTopics(courseId)
            if (topicsResult is Result.Failure) {
                _state.value = UiState.Success(
                    base.copy(isGenerating = false, generationError = topicsResult.error)
                )
                return@launch
            }

            val topicIds = (topicsResult as Result.Success).value
                .sortedBy { it.orderIndex }
                .map { it.id }
                .distinct()
                .take(MAX_TOPICS)

            if (topicIds.isEmpty()) {
                _state.value = UiState.Success(
                    base.copy(
                        isGenerating = false,
                        generationError = AppError.Validation(
                            field = "topics",
                            issue = "Este curso todavía no tiene temas publicados en el temario."
                        )
                    )
                )
                return@launch
            }

            when (val genResult = studyRepository.generateStudyItems(courseId, topicIds)) {
                is Result.Success -> {
                    val flashcards = (studyRepository.getFlashcards(courseId) as? Result.Success)?.value.orEmpty()
                    val quizQuestions = (studyRepository.getQuizQuestions(courseId) as? Result.Success)?.value.orEmpty()
                    _state.value = UiState.Success(
                        base.copy(
                            flashcardCount = flashcards.size,
                            quizQuestion = quizQuestions.firstOrNull(),
                            quizQuestionCount = quizQuestions.size,
                            isGenerating = false,
                            generationError = null
                        )
                    )
                }
                is Result.Failure -> {
                    _state.value = UiState.Success(
                        base.copy(isGenerating = false, generationError = genResult.error)
                    )
                }
            }
        }
    }

    private companion object {
        const val MAX_TOPICS = 10
    }
}
