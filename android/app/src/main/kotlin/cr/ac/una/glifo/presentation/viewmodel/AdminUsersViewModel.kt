package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.AdminRole
import cr.ac.una.glifo.domain.model.AdminUser
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Pantalla Usuarios del admin: todas las cuentas y el cambio de rol. */
@HiltViewModel
class AdminUsersViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<AdminUser>>>(UiState.Loading)
    val state: StateFlow<UiState<List<AdminUser>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val result = adminRepository.getUsers()) {
                is Result.Success -> UiState.Success(result.value)
                is Result.Failure -> UiState.Error(result.error)
            }
        }
    }

    /** Cambia el rol y reemplaza esa fila en la lista sin volver a cargar todo. */
    suspend fun changeRole(userId: Long, role: GlifoRole): Result<AdminUser> {
        val result = adminRepository.changeUserRole(userId, role)
        val current = _state.value
        if (result is Result.Success && current is UiState.Success) {
            _state.value = UiState.Success(current.data.map { if (it.id == userId) result.value else it })
        }
        return result
    }
}

/** Pantalla Roles del admin: solo lectura. */
@HiltViewModel
class AdminRolesViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<AdminRole>>>(UiState.Loading)
    val state: StateFlow<UiState<List<AdminRole>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val result = adminRepository.getRoles()) {
                is Result.Success -> UiState.Success(result.value)
                is Result.Failure -> UiState.Error(result.error)
            }
        }
    }
}
