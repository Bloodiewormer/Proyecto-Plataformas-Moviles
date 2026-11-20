package cr.ac.una.glifo.presentation.viewmodel

import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CourseDetail
import cr.ac.una.glifo.domain.model.CourseStats
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.NoteRepository
import cr.ac.una.glifo.domain.repository.SessionRepository
import cr.ac.una.glifo.domain.repository.StudyRepository
import cr.ac.una.glifo.domain.repository.TeacherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CourseDetailUiData(val course: CourseDetail?, val stats: CourseStats)

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val activeCourseStore: ActiveCourseStore,
    // null en pruebas. Con ellos los conteos salen de lo mismo que usa el Inicio.
    private val noteRepository: NoteRepository? = null,
    private val studyRepository: StudyRepository? = null,
    private val teacherRepository: TeacherRepository? = null,
    private val sessionRepository: SessionRepository? = null
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<CourseDetailUiData>>(UiState.Loading)
    val state: StateFlow<UiState<CourseDetailUiData>> = _state.asStateFlow()

    private var loadedFor: Long? = null

    fun load(courseId: Long) {
        if (loadedFor == courseId) return
        loadedFor = courseId
        // Abrir un curso lo vuelve el curso activo del inicio y del menú.
        activeCourseStore.set(courseId)
        viewModelScope.launch {
            _state.value = UiState.Loading
            val statsResult = courseRepository.getCourseStats(courseId)
            val stats = (statsResult as? Result.Success)?.value
            if (stats == null) {
                _state.value = UiState.Error((statsResult as Result.Failure).error)
                return@launch
            }
            val courseResult = courseRepository.getCourseDetail(courseId)
            (courseResult as? Result.Success)?.value?.let { activeCourseStore.set(it.id, it.name) }
            val role = sessionRepository?.currentUser?.value?.role
            // Evita 403 en /coverage/gaps cuando el visor es estudiante o aún no tiene rol conocido.
            val group = if (role != null && role != GlifoRole.Student) {
                (teacherRepository?.getGroupCoverage(courseId) as? Result.Success)?.value
            } else {
                null
            }
            val baseStats = group?.let { stats.withGroup(it) } ?: stats
            // La cobertura no trae apuntes ni pendientes (venían en 0 mientras el Inicio mostraba 3 apuntes
            // y 1 flashcard + 1 quiz). Se cuentan con los mismos repositorios que usa el Inicio.
            val noteCount = (noteRepository?.getNoteMetaList(courseId) as? Result.Success)?.value?.size
            val flashcards = (studyRepository?.getFlashcards(courseId) as? Result.Success)?.value?.size
            val hasQuiz = studyRepository?.getQuizQuestion(courseId) is Result.Success
            val pending = flashcards?.let { it + if (hasQuiz) 1 else 0 }
            val fixedStats = baseStats.copy(
                noteCount = noteCount ?: baseStats.noteCount,
                pendingCount = pending ?: baseStats.pendingCount
            )
            _state.value = UiState.Success(
                CourseDetailUiData(course = (courseResult as? Result.Success)?.value, stats = fixedStats)
            )
        }
    }
}
