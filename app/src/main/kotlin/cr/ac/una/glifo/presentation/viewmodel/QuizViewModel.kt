package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.domain.model.QuizQuestion
import cr.ac.una.glifo.domain.model.QuizResultInfo
import cr.ac.una.glifo.domain.model.QuizResultRow
import cr.ac.una.glifo.domain.repository.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * QuizScreen: sigue al curso activo, registra la opción elegida por el usuario en cada
 * pregunta y al finalizar calcula y guarda localmente [QuizResultInfo] (#44, #45).
 */
@HiltViewModel
class QuizViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
    private val activeCourseStore: ActiveCourseStore
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<QuizQuestion>>>(UiState.Loading)
    val state: StateFlow<UiState<List<QuizQuestion>>> = _state.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    /** Índice de la opción elegida por cada índice de pregunta (0-based). */
    private val selectedAnswers = mutableMapOf<Int, Int>()

    init { followActiveCourse(activeCourseStore, ::fetch) }

    fun load() {
        viewModelScope.launch { fetch(activeCourseStore.activeCourseId.value) }
    }

    private suspend fun fetch(courseId: Long?) {
        _currentIndex.value = 0
        selectedAnswers.clear()
        if (courseId == null) {
            _state.value = UiState.Empty
            return
        }
        _state.value = UiState.Loading
        _state.value = studyRepository.getQuizQuestions(courseId).toUiStateOrEmpty()
    }

    /**
     * Registra la opción elegida en la pregunta actual (o en [questionIndex]) y persiste
     * el resultado actualizado en [StudyRepository] para el curso activo.
     */
    fun submitAnswer(selectedOptionIndex: Int, questionIndex: Int = _currentIndex.value) {
        selectedAnswers[questionIndex] = selectedOptionIndex
        val courseId = activeCourseStore.activeCourseId.value ?: return
        val result = buildQuizResult() ?: return
        viewModelScope.launch {
            studyRepository.saveQuizResult(courseId, result)
        }
    }

    /**
     * Calcula el [QuizResultInfo] con las respuestas reales registradas hasta el momento.
     */
    fun buildQuizResult(): QuizResultInfo? {
        val questions = (_state.value as? UiState.Success)?.data ?: return null
        if (questions.isEmpty()) return null

        val total = questions.size
        var correctCount = 0
        val reviewRows = ArrayList<QuizResultRow>(total)
        val failedQuestions = ArrayList<QuizQuestion>()

        questions.forEachIndexed { index, question ->
            val chosen = selectedAnswers[index]
            val isCorrect = chosen != null && chosen == question.correctIndex
            if (isCorrect) {
                correctCount++
            } else {
                failedQuestions.add(question)
            }
            reviewRows.add(
                QuizResultRow(
                    correct = isCorrect,
                    label = "${index + 1} · ${question.topicLabel}"
                )
            )
        }

        val failedCount = total - correctCount
        val headline = if (failedQuestions.isEmpty()) {
            "¡Todo correcto!"
        } else {
            val worstTopic = failedQuestions
                .groupBy { it.topicLabel }
                .maxByOrNull { it.value.size }
                ?.key
                .orEmpty()
            "Repasá: $worstTopic"
        }

        val distinctFailedTopics = failedQuestions.map { it.topicLabel }.distinct()
        val subtext = if (failedCount > 0 && distinctFailedTopics.size == 1) {
            "Fallaste $failedCount de $total. Los $failedCount fallos son del mismo tema."
        } else {
            "Fallaste $failedCount de $total."
        }

        return QuizResultInfo(
            score = correctCount,
            total = total,
            headline = headline,
            subtext = subtext,
            review = reviewRows,
            rescheduledCount = null,
            daysUntilReturn = null
        )
    }

    /**
     * Calcula y guarda en [StudyRepository] el resultado final del quiz para el curso activo.
     */
    fun finishQuiz(): QuizResultInfo? {
        val result = buildQuizResult() ?: return null
        val courseId = activeCourseStore.activeCourseId.value
        if (courseId != null) {
            viewModelScope.launch {
                studyRepository.saveQuizResult(courseId, result)
            }
        }
        return result
    }

    fun nextQuestion(): Boolean {
        val questions = (_state.value as? UiState.Success)?.data ?: return false
        val next = _currentIndex.value + 1
        return if (next < questions.size) {
            _currentIndex.value = next
            true
        } else {
            false
        }
    }
}
