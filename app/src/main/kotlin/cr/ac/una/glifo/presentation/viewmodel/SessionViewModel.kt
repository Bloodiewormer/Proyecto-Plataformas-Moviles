package cr.ac.una.glifo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.una.glifo.core.datastore.ActiveCourseStore
import cr.ac.una.glifo.core.network.SessionEvent
import cr.ac.una.glifo.core.network.SessionEventBus
import cr.ac.una.glifo.domain.model.User
import cr.ac.una.glifo.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
    private val activeCourseStore: ActiveCourseStore,
    private val sessionEventBus: SessionEventBus
) : ViewModel() {

    val currentUser: StateFlow<User?> = sessionRepository.currentUser
    val profilePhotoUri: StateFlow<String?> = sessionRepository.profilePhotoUri
    val sessionEvents: Flow<SessionEvent> = sessionEventBus.events

    fun login(user: User) = sessionRepository.login(user)

    fun logout() {
        activeCourseStore.clear()
        viewModelScope.launch {
            sessionRepository.logout()
        }
    }

    fun setProfilePhotoUri(uri: String?) = sessionRepository.setProfilePhotoUri(uri)
}
