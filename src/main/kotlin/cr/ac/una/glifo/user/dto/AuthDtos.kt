package cr.ac.una.glifo.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank(message = "El correo es obligatorio") val email: String,
    @field:NotBlank(message = "La contraseña es obligatoria") val password: String
)

data class RegisterRequest(
    @field:NotBlank(message = "El correo es obligatorio")
    @field:Email(message = "El correo no es válido")
    val email: String,
    @field:NotBlank(message = "La contraseña es obligatoria")
    @field:Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    val password: String,
    val firstName: String?,
    val lastName: String?
)

/** Se devuelve tal cual, sin el envoltorio `data`: es el formato que espera la app. */
data class AuthResponse(
    val token: String,
    val user: UserResponse
)

data class UserResponse(
    val id: Long,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    /** Rol principal con el nombre que usa la app: "Student", "Teacher" o "Admin". */
    val role: String,
    /** Todos los roles tal como están en la base (ROLE_STUDENT, …). */
    val roles: List<String>
)
