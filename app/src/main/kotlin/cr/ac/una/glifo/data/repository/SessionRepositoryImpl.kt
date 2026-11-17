package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.datastore.ProfilePhotoStore
import cr.ac.una.glifo.core.datastore.TokenManager
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.domain.model.User
import cr.ac.una.glifo.domain.repository.SessionRepository
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the currently logged-in user for the lifetime of the process. In-memory session state
 * backed by [TokenManager] for persisted JWT and auth credentials. The profile photo is cached
 * per user on the device and mirrored on the server so it follows the account across devices.
 */
@Singleton
class SessionRepositoryImpl internal constructor(
    private val tokenManager: TokenManager,
    private val localUserDataCleaner: LocalUserDataCleaner?,
    private val profilePhotoStore: ProfilePhotoStore?,
    // Lazy: GlifoApi usa OkHttp, que usa AuthInterceptor, que a su vez usa este repositorio.
    private val api: Lazy<GlifoApi>?,
    private val scope: CoroutineScope
) : SessionRepository {

    @Inject
    constructor(
        tokenManager: TokenManager,
        localUserDataCleaner: LocalUserDataCleaner?,
        profilePhotoStore: ProfilePhotoStore?,
        api: Lazy<GlifoApi>
    ) : this(tokenManager, localUserDataCleaner, profilePhotoStore, api, CoroutineScope(SupervisorJob() + Dispatchers.IO))

    constructor(tokenManager: TokenManager) : this(
        tokenManager = tokenManager,
        localUserDataCleaner = null,
        profilePhotoStore = null,
        api = null,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    )

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _profilePhotoUri = MutableStateFlow<String?>(null)
    override val profilePhotoUri: StateFlow<String?> = _profilePhotoUri.asStateFlow()

    override fun login(user: User) {
        _currentUser.value = user
        _profilePhotoUri.value = profilePhotoStore?.find(user.id)
        scope.launch { syncProfilePhoto(user.id) }
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

    override suspend fun saveProfilePhoto(sourceUri: String) {
        val userId = _currentUser.value?.id ?: return
        val saved = profilePhotoStore?.save(userId, sourceUri) ?: return
        if (_currentUser.value?.id == userId) _profilePhotoUri.value = saved
        upload(saved)
    }

    /**
     * El servidor manda: si tiene foto, reemplaza la copia local. Si no tiene y el teléfono sí
     * (elegida antes de existir el endpoint o cuando falló la subida), se sube. Sin red se
     * queda la copia local.
     */
    internal suspend fun syncProfilePhoto(userId: Long) {
        val remote = api?.get() ?: return
        val store = profilePhotoStore ?: return
        runCatching {
            val response = remote.getAvatar()
            when {
                response.isSuccessful -> {
                    val bytes = response.body()?.bytes() ?: return
                    val path = store.saveDownloaded(userId, bytes) ?: return
                    if (_currentUser.value?.id == userId) _profilePhotoUri.value = path
                }
                response.code() == 404 -> store.find(userId)?.let { upload(it) }
            }
        }
    }

    private suspend fun upload(path: String) {
        val remote = api?.get() ?: return
        runCatching {
            val body = File(path).readBytes().toRequestBody(JPEG.toMediaType())
            remote.uploadAvatar(MultipartBody.Part.createFormData("file", "avatar.jpg", body))
        }
    }

    private companion object {
        const val JPEG = "image/jpeg"
    }
}
