package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.DemoAccountHint
import cr.ac.una.glifo.domain.model.User

/** Login/registration. Backed today by the in-memory dummy auth provider (temporary). */
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(fullName: String, email: String, password: String): Result<User>
    suspend fun emailExists(email: String): Result<Boolean>

    /** Dummy-phase only — see [DemoAccountHint]. */
    suspend fun getDemoAccountHints(): Result<List<DemoAccountHint>>
}
