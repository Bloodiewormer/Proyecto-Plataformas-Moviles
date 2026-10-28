package cr.ac.una.glifo.domain.repository

import kotlinx.coroutines.flow.StateFlow
import cr.ac.una.glifo.domain.model.User

/**
 * Holds the currently logged-in user for the lifetime of the process, and the (test-only,
 * locally-picked) profile photo. Purely in-memory — same behavior as the previous
 * `SessionManager` singleton — but now declared as a domain contract and backed by a
 * Hilt-injected `@Singleton` implementation in `data/`, instead of a raw object screens
 * reached into directly. Photo is a `String?` (URI text) rather than `android.net.Uri` so
 * this interface stays framework-free; the presentation layer parses it back into a `Uri`.
 */
interface SessionRepository {
    val currentUser: StateFlow<User?>
    val profilePhotoUri: StateFlow<String?>

    fun login(user: User)
    suspend fun logout()
    fun setProfilePhotoUri(uri: String?)
}
