package cr.ac.una.glifo.data.dto

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val email: String, val password: String, val firstName: String, val lastName: String)
data class AuthResponse(val token: String, val user: UserDto)

/** POST auth/change-password (camelCase, igual que login y registro). Responde 204. */
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

/**
 * [role] mirrors [cr.ac.una.glifo.domain.model.GlifoRole]'s names ("Student", "Teacher",
 * "Admin") as a plain string — Gson deserializes it straight off the mock's JSON body, and
 * [cr.ac.una.glifo.data.repository.AuthRepositoryImpl] parses it into the enum. No document
 * fixes this field's exact name yet (there's no real backend team contract), so it's the
 * mock (Mockoon route) that defines it, not the other way around.
 */
data class UserDto(val id: Long, val email: String, val firstName: String?, val lastName: String?, val role: String)
