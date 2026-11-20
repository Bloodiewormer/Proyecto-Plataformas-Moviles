package cr.ac.una.glifo.domain.model

/**
 * The logged-in account. Backed today by [cr.ac.una.glifo.data.datasource.dummy.DummyAuthProvider]
 * (in-memory, no real backend yet); once auth moves to the real API this stays the same
 * shape and only the data-layer source changes.
 */
data class User(
    val id: Long,
    val fullName: String,
    val email: String,
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
 * Dummy-phase-only affordance: the login screen shows one tappable chip per seeded
 * account so the 3 roles can be exercised without typing credentials. Carries a password
 * on purpose (test credentials, not the current session's) — this type must never be used
 * anywhere outside that autofill feature, and disappears entirely once there's a real
 * backend (see AuthRepository.getDemoAccountHints).
 */
data class DemoAccountHint(
    val email: String,
    val password: String,
    val role: GlifoRole
)
