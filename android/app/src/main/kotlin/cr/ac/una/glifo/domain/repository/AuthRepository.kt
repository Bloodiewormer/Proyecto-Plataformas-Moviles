package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.DemoAccountHint
import cr.ac.una.glifo.domain.model.User

/** Login/registration. Backed today by the in-memory dummy auth provider (temporary). */
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(fullName: String, email: String, password: String): Result<User>

    /**
     * Cambia la contraseña del usuario con sesión. Por defecto falla, para que los repositorios
     * falsos de las pruebas no tengan que implementarlo.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
        Result.Failure(AppError.Unknown)

    /** "Olvidé mi contraseña": cambia la contraseña sin sesión, solo con el correo. */
    suspend fun forgotPassword(email: String, newPassword: String): Result<Unit> =
        Result.Failure(AppError.Unknown)

    /** Dummy-phase only — see [DemoAccountHint]. */
    suspend fun getDemoAccountHints(): Result<List<DemoAccountHint>>
}
