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

/** Screen J1: Usuarios Admin. */
@HiltViewModel
class AdminUsersViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<AdminUserRow>>>(UiState.Loading)
    val state: StateFlow<UiState<List<AdminUserRow>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = adminRepository.getUsers()) {
                is Result.Success -> _state.value = UiState.Success(result.value)
                is Result.Failure -> _state.value = UiState.Error(result.error)
            }
        }
    }

    fun addUser(name: String, email: String, role: String) {
        viewModelScope.launch {
            adminRepository.addUser(name, email, role)
            load()
        }
    }
}
