package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.CourseDetail
import cr.ac.una.glifo.domain.model.CoverageSummary
import cr.ac.una.glifo.domain.model.CoverageTopic
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.CoverageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoverageUiData(val course: CourseDetail?, val summary: CoverageSummary, val topics: List<CoverageTopic>)

@HiltViewModel
class CoverageViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val coverageRepository: CoverageRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<CoverageUiData>>(UiState.Loading)
    val state: StateFlow<UiState<CoverageUiData>> = _state.asStateFlow()

    private var loadedFor: Long? = null

    fun load(courseId: Long) {
        if (loadedFor == courseId) return
        loadedFor = courseId
        viewModelScope.launch {
            _state.value = UiState.Loading
            val summaryResult = coverageRepository.getSummary(courseId)
            val summary = (summaryResult as? Result.Success)?.value
            if (summary == null) {
                _state.value = UiState.Error((summaryResult as Result.Failure).error)
                return@launch
            }
            val courseResult = courseRepository.getCourseDetail(courseId)
            val topicsResult = coverageRepository.getTopics(courseId)
            _state.value = UiState.Success(
                CoverageUiData(
                    course = (courseResult as? Result.Success)?.value,
                    summary = summary,
                    topics = (topicsResult as? Result.Success)?.value.orEmpty()
                )
            )
        }
    }
}
