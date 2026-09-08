package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.model.QuizResultInfo
import cr.ac.una.glifo.domain.repository.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** QuizResultScreen. Same hardcoded course the dummy screen always used. */
@HiltViewModel
class QuizResultViewModel @Inject constructor(
    private val studyRepository: StudyRepository
) : ViewModel() {

    companion object { private const val COURSE_ID = 1L }

    private val _state = MutableStateFlow<UiState<QuizResultInfo>>(UiState.Loading)
    val state: StateFlow<UiState<QuizResultInfo>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = studyRepository.getQuizResult(COURSE_ID).toUiState()
        }
    }
}
