package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.AdminUserRow
import cr.ac.una.glifo.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUserDetailUiData(
    val user: AdminUserRow,
    val roleKey: String,
    val privileges: List<String>
)

/** Screen J2: Detalle Usuario Admin. */
@HiltViewModel
class AdminUserDetailViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<AdminUserDetailUiData>>(UiState.Loading)
    val state: StateFlow<UiState<AdminUserDetailUiData>> = _state.asStateFlow()

    private var loadedFor: String? = null

    fun load(userName: String) {
        if (loadedFor == userName) return
        loadedFor = userName
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val userResult = adminRepository.findUser(userName)) {
                is Result.Failure -> _state.value = UiState.Error(userResult.error)
                is Result.Success -> {
                    val user = userResult.value
                    val roleKey = when (user.role) {
                        "DOCENTE" -> "Docente"
                        "ADMIN" -> "Administrador"
                        else -> "Estudiante"
                    }
                    val privileges = (adminRepository.getPrivilegesFor(roleKey) as? Result.Success)?.value.orEmpty()
                    _state.value = UiState.Success(AdminUserDetailUiData(user, roleKey, privileges))
                }
            }
        }
    }

    suspend fun setActive(userName: String, active: Boolean): Result<Unit> =
        adminRepository.setActive(userName, active)
}
