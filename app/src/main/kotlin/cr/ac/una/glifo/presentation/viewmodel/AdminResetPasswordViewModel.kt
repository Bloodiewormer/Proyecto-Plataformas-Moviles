package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Diálogo "Restablecer contraseña" del admin. Devuelve la contraseña temporal. */
@HiltViewModel
class AdminResetPasswordViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    suspend fun resetPassword(email: String): Result<String> =
        adminRepository.resetUserPassword(email)
}
