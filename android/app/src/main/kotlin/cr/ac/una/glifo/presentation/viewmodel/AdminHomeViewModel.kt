package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ApiHealth
import cr.ac.una.glifo.domain.repository.AdminRepository
import cr.ac.una.glifo.domain.repository.CourseRepository
import cr.ac.una.glifo.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminHomeUiData(
    val apiHealth: ApiHealth?,
    val healthError: AppError?,
    val courseCount: Int?,
    val pendingSyncCount: Int
)

/** Screen B4: Inicio Administrador. */
@HiltViewModel
class AdminHomeViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val courseRepository: CourseRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<AdminHomeUiData>>(UiState.Loading)
    val state: StateFlow<UiState<AdminHomeUiData>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val healthResult = adminRepository.checkHealth()
            val studentCoursesResult = courseRepository.getStudentCourses()
            val teacherCoursesResult = courseRepository.getTeacherCourses()
            val pendingResult = syncRepository.getPendingCount()

            val courseCount = if (studentCoursesResult is Result.Failure && teacherCoursesResult is Result.Failure) {
                null
            } else {
                (
                    (studentCoursesResult as? Result.Success)?.value.orEmpty() +
                        (teacherCoursesResult as? Result.Success)?.value.orEmpty()
                    ).distinctBy { it.id }.size
            }

            _state.value = UiState.Success(
                AdminHomeUiData(
                    apiHealth = (healthResult as? Result.Success)?.value,
                    healthError = (healthResult as? Result.Failure)?.error,
                    courseCount = courseCount,
                    pendingSyncCount = (pendingResult as? Result.Success)?.value ?: 0
                )
            )
        }
    }
}
