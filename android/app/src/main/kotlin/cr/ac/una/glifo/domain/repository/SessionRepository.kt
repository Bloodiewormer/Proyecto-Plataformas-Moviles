package cr.ac.una.glifo.domain.repository

import kotlinx.coroutines.flow.StateFlow
import cr.ac.una.glifo.domain.model.User

/**
 * Holds the currently logged-in user for the lifetime of the process, and the path of their
 * profile photo, which is kept only on this device per user.
 */
interface SessionRepository {
    val currentUser: StateFlow<User?>
    val profilePhotoUri: StateFlow<String?>

    fun login(user: User)
    suspend fun logout()

    /** Token vencido: cierra la sesión sin borrar datos locales pendientes de sincronizar. */
    suspend fun expireSession() = logout()

    /** Copia la imagen elegida (URI del selector) como foto del usuario actual. */
    suspend fun saveProfilePhoto(sourceUri: String)

    /** Restaura la sesión persistida si hay token y usuario guardados. */
    suspend fun restoreSession(): User?
}
