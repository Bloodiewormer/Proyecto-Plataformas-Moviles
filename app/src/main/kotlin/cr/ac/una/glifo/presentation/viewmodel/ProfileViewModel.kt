package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.domain.repository.AiCostRepository
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileStatsUiData(val pagesCaptured: Int, val resolvedPercent: Int, val courseCount: Int)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val aiCostRepository: AiCostRepository,
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ProfileStatsUiData>>(UiState.Loading)
    val state: StateFlow<UiState<ProfileStatsUiData>> = _state.asStateFlow()

    private var loadedFor: GlifoRole? = null

    fun load(role: GlifoRole) {
        if (loadedFor == role) return
        loadedFor = role
        viewModelScope.launch {
            _state.value = UiState.Loading
            val pagesCaptured = (noteRepository.getNoteMetaList() as? Result.Success)?.value.orEmpty().sumOf { it.pageCount }
            val resolvedPercent = (aiCostRepository.getStats(role) as? Result.Success)?.value?.headlinePercent ?: 0
            val courseCount = when (role) {
                GlifoRole.Teacher -> (courseRepository.getTeacherCourses() as? Result.Success)?.value.orEmpty().size
                GlifoRole.Admin -> (
                    (courseRepository.getStudentCourses() as? Result.Success)?.value.orEmpty() +
                        (courseRepository.getTeacherCourses() as? Result.Success)?.value.orEmpty()
                    ).distinctBy { it.id }.size
                GlifoRole.Student -> (courseRepository.getStudentCourses() as? Result.Success)?.value.orEmpty().size
            }
            _state.value = UiState.Success(ProfileStatsUiData(pagesCaptured, resolvedPercent, courseCount))
        }
    }
}
