package cr.ac.una.glifo.core.data.dummy

import cr.ac.una.glifo.core.ui.component.GlifoRole

/**
 * Dummy (local-only) account model. Mirrors the shape a real `UserDto`/`AuthResponse`
 * would take once there's a backend — swapping this provider for real network calls
 * later shouldn't require touching the screens that consume it.
 */
data class DummyUser(
    val id: Long,
    val fullName: String,
    val email: String,
    val password: String,
    val role: GlifoRole
) {
    val initials: String
        get() = fullName
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifEmpty { "??" }
}

/**
 * In-memory dummy auth "backend". Seeded with one account per role so login can be
 * exercised end-to-end without a real API. Registration appends to this same list
 * (session-only — resets on app restart, which is fine while there's no real backend).
 */
object DummyAuthProvider {

    private val users = mutableListOf(
        DummyUser(
            id = 1L,
            fullName = "Brandon Brenes",
            email = "estudiante@est.una.ac.cr",
            password = "Estudiante123",
            role = GlifoRole.Student
        ),
        DummyUser(
            id = 2L,
            fullName = "Maikol Guzmán",
            email = "docente@una.ac.cr",
            password = "Docente123",
            role = GlifoRole.Teacher
        ),
        DummyUser(
            id = 3L,
            fullName = "Admin Glifo",
            email = "admin@una.ac.cr",
            password = "Admin1234",
            role = GlifoRole.Admin
        )
    )

    /** Credential hints shown on the login screen to make testing the 3 roles trivial. */
    val demoAccounts: List<DummyUser> get() = users.filter { it.id in 1L..3L }

    fun findUser(email: String, password: String): DummyUser? =
        users.find { it.email.equals(email.trim(), ignoreCase = true) && it.password == password }

    fun emailExists(email: String): Boolean =
        users.any { it.email.equals(email.trim(), ignoreCase = true) }

    fun register(fullName: String, email: String, password: String): DummyUser {
        val user = DummyUser(
            id = (users.maxOfOrNull { it.id } ?: 0L) + 1L,
            fullName = fullName.trim(),
            email = email.trim(),
            password = password,
            role = GlifoRole.Student
        )
        users.add(user)
        return user
    }
}
