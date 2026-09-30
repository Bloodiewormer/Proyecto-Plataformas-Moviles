package cr.ac.una.glifo.presentation.viewmodel

import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import androidx.lifecycle.ViewModel
import cr.ac.una.glifo.domain.model.User
import cr.ac.una.glifo.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Thin wrapper around [SessionRepository] so Compose screens reach the logged-in user through
 * `hiltViewModel()` instead of a static singleton (the old `core.data.session.SessionManager`).
 * Safe to request from any screen/NavHost scope: the backing repository is a Hilt `@Singleton`,
 * so every instance of this ViewModel observes the same session state.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val activeCourseStore: ActiveCourseStore
) : ViewModel() {

    val currentUser: StateFlow<User?> = sessionRepository.currentUser
    val profilePhotoUri: StateFlow<String?> = sessionRepository.profilePhotoUri

    fun login(user: User) = sessionRepository.login(user)

    fun logout() {
        sessionRepository.logout()
        activeCourseStore.clear()
    }

    fun setProfilePhotoUri(uri: String?) = sessionRepository.setProfilePhotoUri(uri)
}
