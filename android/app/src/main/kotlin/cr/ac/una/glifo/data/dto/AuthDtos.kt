package cr.ac.una.glifo.data.dto

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val email: String, val password: String, val firstName: String, val lastName: String)
data class AuthResponse(val token: String, val user: UserDto)

/** POST auth/change-password (camelCase, igual que login y registro). Responde 204. */
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

/** POST auth/forgot-password, sin token. 204 si cambió; 404 si el correo no existe. */
data class ForgotPasswordRequest(val email: String, val newPassword: String)

/**
 * [role] mirrors [cr.ac.una.glifo.domain.model.GlifoRole]'s names ("Student", "Teacher",
 * "Admin") as a plain string — Gson deserializes it straight off the mock's JSON body, and
 * [cr.ac.una.glifo.data.repository.AuthRepositoryImpl] parses it into the enum. No document
 * fixes this field's exact name yet (there's no real backend team contract), so it's the
 * mock (Mockoon route) that defines it, not the other way around.
 */
data class UserDto(val id: Long, val email: String, val firstName: String?, val lastName: String?, val role: String)

/** POST admin/users/reset-password (solo admin). */
data class AdminResetPasswordRequest(val email: String)

/** PUT admin/users/{id}/role. [role] = "Student", "Teacher" o "Admin". */
data class AdminChangeRoleRequest(val role: String)

/** GET admin/users/roles. */
data class AdminRoleDto(val name: String, val description: String?, val privileges: List<String>, val userCount: Int)

/** La temporal llega en claro una sola vez, para que el admin se la pase al usuario. */
data class AdminResetPasswordResponse(val email: String, val temporaryPassword: String)
