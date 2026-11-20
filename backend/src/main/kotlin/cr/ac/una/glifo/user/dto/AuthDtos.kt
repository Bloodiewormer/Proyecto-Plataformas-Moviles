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
    @field:Size(max = 255, message = "El correo admite como máximo 255 caracteres")
    val email: String,
    @field:NotBlank(message = "La contraseña es obligatoria")
    @field:Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres")
    val password: String,
    @field:Size(max = 100, message = "El nombre admite como máximo 100 caracteres")
    val firstName: String?,
    @field:Size(max = 100, message = "El apellido admite como máximo 100 caracteres")
    val lastName: String?
)

/** Cambio de contraseña del usuario autenticado. Mismas reglas de largo que el registro. */
data class ChangePasswordRequest(
    @field:NotBlank(message = "La contraseña actual es obligatoria")
    val currentPassword: String,
    @field:NotBlank(message = "La contraseña nueva es obligatoria")
    @field:Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres")
    val newPassword: String
)

/**
 * "Olvidé mi contraseña" sin sesión: basta el correo. Decisión del equipo para la demo, sin
 * verificación por correo; cualquiera que sepa el correo puede cambiarla.
 */
data class ForgotPasswordRequest(
    @field:NotBlank(message = "El correo es obligatorio")
    @field:Email(message = "El correo no es válido")
    val email: String,
    @field:NotBlank(message = "La contraseña nueva es obligatoria")
    @field:Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres")
    val newPassword: String
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

/** El admin restablece la contraseña de otra cuenta (no hay envío de correos todavía). */
data class AdminResetPasswordRequest(
    @field:NotBlank(message = "El correo es obligatorio")
    @field:Email(message = "El correo no es válido")
    val email: String
)

/** El admin cambia el rol de otra cuenta. Valores: "Student", "Teacher" o "Admin" (los de UserResponse.role). */
data class AdminChangeRoleRequest(
    @field:NotBlank(message = "El rol es obligatorio")
    val role: String
)

/** Un rol con sus privilegios y cuántas cuentas lo tienen (pantalla Roles del admin). */
data class AdminRoleResponse(
    val name: String,
    val description: String?,
    val privileges: List<String>,
    val userCount: Int
)

/** La contraseña temporal se muestra una sola vez al admin, para que se la pase al usuario. */
data class AdminResetPasswordResponse(
    val email: String,
    val temporaryPassword: String
)
