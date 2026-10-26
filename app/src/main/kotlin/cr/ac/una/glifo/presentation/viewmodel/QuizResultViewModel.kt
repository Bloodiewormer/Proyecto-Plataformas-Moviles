package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.QuizResultInfo
import cr.ac.una.glifo.domain.repository.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * QuizResultScreen: sigue al curso activo y lee el último resultado real guardado en
 * [StudyRepository]. Si no hay resultado guardado para el curso, expone [UiState.Empty].
 */
@HiltViewModel
class QuizResultViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
    private val activeCourseStore: ActiveCourseStore
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<QuizResultInfo>>(UiState.Loading)
    val state: StateFlow<UiState<QuizResultInfo>> = _state.asStateFlow()

    init { followActiveCourse(activeCourseStore, ::fetch) }

    fun load() {
        viewModelScope.launch { fetch(activeCourseStore.activeCourseId.value) }
    }

    private suspend fun fetch(courseId: Long?) {
        if (courseId == null) {
            _state.value = UiState.Empty
            return
        }
        _state.value = UiState.Loading
        _state.value = when (val result = studyRepository.getQuizResult(courseId)) {
            is Result.Success -> {
                val info = result.value
                if (info == null) UiState.Empty else UiState.Success(info)
            }
            is Result.Failure -> UiState.Error(result.error)
        }
    }
}
