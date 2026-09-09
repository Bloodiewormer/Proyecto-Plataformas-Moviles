package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.User
import cr.ac.una.glifo.domain.repository.AuthRepository
import cr.ac.una.glifo.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    suspend fun emailExists(email: String): Boolean {
        val result = authRepository.emailExists(email)
        return (result as? Result.Success)?.value ?: false
    }

    /** Registers and, on success, stores the new user in [SessionRepository]. */
    suspend fun register(fullName: String, email: String, password: String): Result<User> {
        val result = authRepository.register(fullName, email, password)
        if (result is Result.Success) sessionRepository.login(result.value)
        return result
    }
}
