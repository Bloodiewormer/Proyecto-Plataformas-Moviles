package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.domain.model.User
import cr.ac.una.glifo.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the currently logged-in user for the lifetime of the process. Purely in-memory —
 * same behavior as the previous `core.data.session.SessionManager` object — but now a
 * Hilt `@Singleton` behind the [SessionRepository] contract instead of a screen-reachable
 * static singleton, so ViewModels depend on the interface like any other repository.
 * Once there's a real backend this is the seam to back with a persisted session
 * (e.g. via `core.datastore.TokenManager`).
 */
@Singleton
class SessionRepositoryImpl @Inject constructor() : SessionRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _profilePhotoUri = MutableStateFlow<String?>(null)
    override val profilePhotoUri: StateFlow<String?> = _profilePhotoUri.asStateFlow()

    override fun login(user: User) {
        _currentUser.value = user
        _profilePhotoUri.value = null
    }

    override fun logout() {
        _currentUser.value = null
        _profilePhotoUri.value = null
    }

    override fun setProfilePhotoUri(uri: String?) {
        _profilePhotoUri.value = uri
    }
}
