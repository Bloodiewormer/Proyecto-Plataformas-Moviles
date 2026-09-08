package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.model.Flashcard
import cr.ac.una.glifo.domain.repository.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** FlashcardScreen. Same hardcoded course the dummy screen always used. */
@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val studyRepository: StudyRepository
) : ViewModel() {

    companion object { private const val COURSE_ID = 1L }

    private val _state = MutableStateFlow<UiState<List<Flashcard>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Flashcard>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = studyRepository.getFlashcards(COURSE_ID).toUiStateOrEmpty()
        }
    }
}
