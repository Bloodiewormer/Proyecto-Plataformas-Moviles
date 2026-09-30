package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CourseDetail
import cr.ac.una.glifo.domain.model.QuizQuestion
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudyHomeUiData(val course: CourseDetail?, val flashcardCount: Int, val quizQuestion: QuizQuestion)

/** StudyScreen (the "Estudio" entry point — flashcards + quiz cards for a course). */
@HiltViewModel
class StudyHomeViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<StudyHomeUiData>>(UiState.Loading)
    val state: StateFlow<UiState<StudyHomeUiData>> = _state.asStateFlow()

    private var loadedFor: Long? = null
    private var loadJob: Job? = null

    fun load(courseId: Long) {
        if (loadedFor == courseId && _state.value is UiState.Success) return
        loadedFor = courseId
        // Al cambiar de curso se cancela la carga anterior: si terminara después, pisaría la del nuevo.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = UiState.Loading
            val quizResult = studyRepository.getQuizQuestion(courseId)
            val quiz = (quizResult as? Result.Success)?.value
            if (quiz == null) {
                _state.value = UiState.Error((quizResult as Result.Failure).error)
                return@launch
            }
            val courseResult = courseRepository.getCourseDetail(courseId)
            val flashcardsResult = studyRepository.getFlashcards(courseId)
            _state.value = UiState.Success(
                StudyHomeUiData(
                    course = (courseResult as? Result.Success)?.value,
                    flashcardCount = (flashcardsResult as? Result.Success)?.value?.size ?: 0,
                    quizQuestion = quiz
                )
            )
        }
    }
}
