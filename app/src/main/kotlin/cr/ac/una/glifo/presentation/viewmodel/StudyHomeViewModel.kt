package cr.ac.una.glifo.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CourseDetail
import cr.ac.una.glifo.domain.model.Flashcard
import cr.ac.una.glifo.domain.model.QuizQuestion
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudyHomeUiData(
    val course: CourseDetail?,
    val flashcardCount: Int,
    val flashcardTopicCount: Int = 0,
    val quizQuestion: QuizQuestion?,
    val quizQuestionCount: Int = 0,
    val quizTopicCodes: List<String> = emptyList(),
    val isGenerating: Boolean = false,
    val generationError: AppError? = null
) {
    /**
     * Resumen de códigos de temas del quiz (máximo 3 códigos separados por ", " y "…" si hay más).
     */
    val quizTopicsSummary: String
        get() = when {
            quizTopicCodes.isEmpty() -> ""
            quizTopicCodes.size <= MAX_SUMMARY_TOPICS -> quizTopicCodes.joinToString(", ")
            else -> quizTopicCodes.take(MAX_SUMMARY_TOPICS).joinToString(", ") + "…"
        }

    private companion object {
        const val MAX_SUMMARY_TOPICS = 3
    }
}

/**
 * StudyScreen (el punto de entrada de "Estudio" — tarjetas de flashcards y quiz del curso,
 * más la acción de generación por lotes con IA-02, #44).
 */
@HiltViewModel
class StudyHomeViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val studyRepository: StudyRepository,
    private val activeCourseStore: ActiveCourseStore?,
    @ApplicationContext private val appContext: Context?
) : ViewModel() {

    constructor(
        courseRepository: CourseRepository,
        studyRepository: StudyRepository,
        activeCourseStore: ActiveCourseStore?
    ) : this(courseRepository, studyRepository, activeCourseStore, null)

    constructor(
        courseRepository: CourseRepository,
        studyRepository: StudyRepository
    ) : this(courseRepository, studyRepository, null, null)

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
            val course = resolveCourse(courseId)
            val flashcardsResult = studyRepository.getFlashcards(courseId)
            val quizQuestionsResult = studyRepository.getQuizQuestions(courseId)

            if (flashcardsResult is Result.Failure && quizQuestionsResult is Result.Failure) {
                _state.value = UiState.Error(flashcardsResult.error)
                return@launch
            }

            val flashcards = (flashcardsResult as? Result.Success)?.value.orEmpty()
            val quizQuestions = (quizQuestionsResult as? Result.Success)?.value.orEmpty()

            _state.value = UiState.Success(
                buildUiData(
                    course = course,
                    flashcards = flashcards,
                    quizQuestions = quizQuestions
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
                course = resolveCourse(courseId),
                flashcardCount = 0,
                flashcardTopicCount = 0,
                quizQuestion = null,
                quizQuestionCount = 0,
                quizTopicCodes = emptyList()
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
                            issue = appContext?.getString(R.string.study_error_no_topics)
                                ?: "Este curso todavía no tiene temas publicados en el temario."
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
                        buildUiData(
                            course = base.course,
                            flashcards = flashcards,
                            quizQuestions = quizQuestions
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

    private suspend fun resolveCourse(courseId: Long): CourseDetail? {
        val detail = (courseRepository.getCourseDetail(courseId) as? Result.Success)?.value
        if (detail != null) {
            if (detail.name.isNotBlank()) {
                activeCourseStore?.set(detail.id, detail.name)
            }
            return detail
        }

        val active = activeCourseStore?.activeCourse?.value
        if (active != null && active.id == courseId && !active.name.isNullOrBlank()) {
            return CourseDetail(
                id = active.id,
                name = active.name,
                code = "",
                joinCode = "",
                syllabusStatus = null
            )
        }

        val fromList = (courseRepository.getStudentCourses() as? Result.Success)?.value
            ?.find { it.id == courseId }
        if (fromList != null) {
            activeCourseStore?.set(fromList.id, fromList.name)
            return CourseDetail(
                id = fromList.id,
                name = fromList.name,
                code = fromList.code,
                joinCode = fromList.joinCode,
                syllabusStatus = null
            )
        }

        return null
    }

    private fun buildUiData(
        course: CourseDetail?,
        flashcards: List<Flashcard>,
        quizQuestions: List<QuizQuestion>
    ): StudyHomeUiData {
        val flashcardTopicCount = flashcards
            .map { it.source.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .size
        val quizTopicCodes = quizQuestions
            .map { it.topicTag.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        return StudyHomeUiData(
            course = course,
            flashcardCount = flashcards.size,
            flashcardTopicCount = flashcardTopicCount,
            quizQuestion = quizQuestions.firstOrNull(),
            quizQuestionCount = quizQuestions.size,
            quizTopicCodes = quizTopicCodes,
            isGenerating = false,
            generationError = null
        )
    }

    private companion object {
        const val MAX_TOPICS = 10
    }
}
