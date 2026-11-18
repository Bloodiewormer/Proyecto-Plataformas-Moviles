package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Pantalla "Olvidé mi contraseña" del login. La validación de los campos la hace la pantalla. */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    suspend fun forgotPassword(email: String, newPassword: String): Result<Unit> =
        authRepository.forgotPassword(email, newPassword)
}
