package cr.ac.una.glifo.data.datasource.dummy

import cr.ac.una.glifo.domain.model.GlifoRole

/**
 * Dummy (local-only) account record. Mirrors the shape a real `UserDto`/`AuthResponse`
 * would take once there's a backend — swapping this provider for real network calls
 * later (via [cr.ac.una.glifo.data.datasource.remote.GlifoApi]) shouldn't require
 * touching [cr.ac.una.glifo.data.repository.AuthRepositoryImpl]'s callers. Stays in the
 * data layer (never leaks to domain/presentation) because it carries the password.
 */
data class DummyAccount(
    val id: Long,
    val fullName: String,
    val email: String,
    val password: String,
    val role: GlifoRole
)

/**
 * In-memory dummy auth "backend". Seeded with one account per role so login can be
 * exercised end-to-end without a real API. Registration appends to this same list
 * (session-only — resets on app restart, which is fine while there's no real backend).
 *
 * Temporary by design (see Glifo_Bitacora_Decisiones.md D-24 and the user's explicit
 * instruction to keep dummy data as the data source until a real backend lands) — the
 * seam to delete is this object plus [cr.ac.una.glifo.data.repository.AuthRepositoryImpl]'s
 * use of it, not the repository interface or anything above it.
 */
object DummyAuthProvider {

    private val accounts = mutableListOf(
        DummyAccount(
            id = 1L,
            fullName = "Brandon Brenes",
            email = "estudiante@est.una.ac.cr",
            password = "Estudiante123",
            role = GlifoRole.Student
        ),
        DummyAccount(
            id = 2L,
            fullName = "Maikol Guzmán",
            email = "docente@una.ac.cr",
            password = "Docente123",
            role = GlifoRole.Teacher
        ),
        DummyAccount(
            id = 3L,
            fullName = "Admin Glifo",
            email = "admin@una.ac.cr",
            password = "Admin1234",
            role = GlifoRole.Admin
        )
    )

    /** Credential hints shown on the login screen to make testing the 3 roles trivial. */
    val demoAccounts: List<DummyAccount> get() = accounts.filter { it.id in 1L..3L }

    fun findAccount(email: String, password: String): DummyAccount? =
        accounts.find { it.email.equals(email.trim(), ignoreCase = true) && it.password == password }

    fun emailExists(email: String): Boolean =
        accounts.any { it.email.equals(email.trim(), ignoreCase = true) }

    fun register(fullName: String, email: String, password: String): DummyAccount {
        val account = DummyAccount(
            id = (accounts.maxOfOrNull { it.id } ?: 0L) + 1L,
            fullName = fullName.trim(),
            email = email.trim(),
            password = password,
            role = GlifoRole.Student
        )
        accounts.add(account)
        return account
    }
}
