package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.DemoAccountHint
import cr.ac.una.glifo.domain.model.User
import cr.ac.una.glifo.domain.repository.AuthRepository
import cr.ac.una.glifo.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _demoAccountHints = MutableStateFlow<List<DemoAccountHint>>(emptyList())
    val demoAccountHints: StateFlow<List<DemoAccountHint>> = _demoAccountHints.asStateFlow()

    init {
        viewModelScope.launch {
            val result = authRepository.getDemoAccountHints()
            if (result is Result.Success) _demoAccountHints.value = result.value
        }
    }

    /** Logs in and, on success, stores the user in [SessionRepository]. Caller drives loading UI. */
    suspend fun login(email: String, password: String): Result<User> {
        val result = authRepository.login(email, password)
        if (result is Result.Success) sessionRepository.login(result.value)
        return result
    }
}
