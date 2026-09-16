package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.AdminRoleInfo
import cr.ac.una.glifo.domain.model.AdminUserRow
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
    val roles: List<AdminRoleInfo>,
    val recentSignups: List<AdminUserRow>,
    val courseCount: Int,
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
            val rolesResult = adminRepository.getRoles()
            val roles = (rolesResult as? Result.Success)?.value
            if (roles == null) {
                _state.value = UiState.Error((rolesResult as Result.Failure).error)
                return@launch
            }
            val signupsResult = adminRepository.getRecentSignups()
            val studentCoursesResult = courseRepository.getStudentCourses()
            val teacherCoursesResult = courseRepository.getTeacherCourses()
            val pendingResult = syncRepository.getPendingCount()

            val courseCount = (
                (studentCoursesResult as? Result.Success)?.value.orEmpty() +
                    (teacherCoursesResult as? Result.Success)?.value.orEmpty()
                ).distinctBy { it.id }.size

            _state.value = UiState.Success(
                AdminHomeUiData(
                    roles = roles,
                    recentSignups = (signupsResult as? Result.Success)?.value.orEmpty(),
                    courseCount = courseCount,
                    pendingSyncCount = (pendingResult as? Result.Success)?.value ?: 0
                )
            )
        }
    }
}
