package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.datastore.TokenManager
import cr.ac.una.glifo.domain.model.User
import cr.ac.una.glifo.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the currently logged-in user for the lifetime of the process. In-memory session state
 * backed by [TokenManager] for persisted JWT and auth credentials.
 */
@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val tokenManager: TokenManager,
    private val localUserDataCleaner: LocalUserDataCleaner?
) : SessionRepository {

    constructor(tokenManager: TokenManager) : this(
        tokenManager = tokenManager,
        localUserDataCleaner = null
    )

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _profilePhotoUri = MutableStateFlow<String?>(null)
    override val profilePhotoUri: StateFlow<String?> = _profilePhotoUri.asStateFlow()

    override fun login(user: User) {
        _currentUser.value = user
        _profilePhotoUri.value = null
    }

    override suspend fun logout() {
        _currentUser.value = null
        _profilePhotoUri.value = null
        tokenManager.clearAll()
        localUserDataCleaner?.clearUserData()
    }

    override suspend fun expireSession() {
        _currentUser.value = null
        tokenManager.clearToken()
    }

    override fun setProfilePhotoUri(uri: String?) {
        _profilePhotoUri.value = uri
    }
}
