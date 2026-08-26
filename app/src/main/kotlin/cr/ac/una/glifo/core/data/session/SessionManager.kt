package cr.ac.una.glifo.core.data.session

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cr.ac.una.glifo.core.data.dummy.DummyUser

/**
 * Holds the currently logged-in dummy user for the lifetime of the process.
 *
 * There is no ViewModel/DI-backed session layer yet, so this stays a plain singleton —
 * it is the single thing every screen reads for "who is logged in" (name, email,
 * initials, role) instead of hardcoding a demo identity. Purely in-memory by design:
 * once there's a real backend this is the seam to replace with a persisted session
 * (e.g. backed by `TokenManager`).
 */
object SessionManager {
    var currentUser by mutableStateOf<DummyUser?>(null)
        private set

    /**
     * A locally-picked profile photo, kept only for the current process (test/demo
     * affordance — nothing is uploaded anywhere). Cleared on logout along with the rest
     * of the session.
     */
    var profilePhotoUri by mutableStateOf<Uri?>(null)
        private set

    fun login(user: DummyUser) {
        currentUser = user
        profilePhotoUri = null
    }

    fun logout() {
        currentUser = null
        profilePhotoUri = null
    }

    fun setProfilePhoto(uri: Uri?) {
        profilePhotoUri = uri
    }
}
