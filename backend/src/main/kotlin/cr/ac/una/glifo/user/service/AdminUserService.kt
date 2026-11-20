package cr.ac.una.glifo.user.service

import cr.ac.una.glifo.common.exception.BadRequestException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.user.dto.AdminResetPasswordResponse
import cr.ac.una.glifo.user.dto.UserResponse
import cr.ac.una.glifo.user.dto.AdminRoleResponse
import cr.ac.una.glifo.user.mapper.toResponse
import cr.ac.una.glifo.user.repository.RoleRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

/**
 * Acciones del admin sobre cuentas de otros usuarios: listarlas, cambiarles el rol y
 * restablecerles la contraseña. Esto último es el camino de "Olvidé mi contraseña" mientras no haya envío de correos (Render en plan
 * gratis bloquea SMTP).
 */
@Service
class AdminUserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val roleRepository: RoleRepository,
) {

    @Transactional(readOnly = true)
    fun listUsers(): List<UserResponse> =
        userRepository.findAll().sortedBy { it.email.lowercase() }.map { it.toResponse() }

    @Transactional(readOnly = true)
    fun listRoles(): List<AdminRoleResponse> {
        val users = userRepository.findAll()
        return roleRepository.findAll().sortedBy { it.id }.map { role ->
            AdminRoleResponse(
                name = role.name,
                description = role.description,
                privileges = role.privileges.map { it.name }.sorted(),
                userCount = users.count { user -> user.roles.any { it.name == role.name } }
            )
        }
    }

    /**
     * Deja la cuenta con un solo rol (la app maneja uno por usuario). El admin no puede cambiar
     * el suyo: así siempre queda al menos un administrador.
     */
    @Transactional
    fun changeRole(adminId: Long, userId: Long, role: String): UserResponse {
        val roleName = ROLE_NAMES[role] ?: throw BadRequestException("Rol no válido: usá Student, Teacher o Admin")
        if (userId == adminId) throw BadRequestException("No podés cambiar tu propio rol")
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("Usuario no encontrado") }
        val newRole = roleRepository.findByName(roleName)
            ?: throw ResourceNotFoundException("El rol $roleName no existe")
        user.roles.clear()
        user.roles.add(newRole)
        return userRepository.save(user).toResponse()
    }

    private val random = SecureRandom()

    /**
     * Genera una contraseña temporal, la guarda cifrada y la devuelve en claro una sola vez.
     * El admin no puede usarlo sobre su propia cuenta (para eso está change-password, que pide la
     * actual) ni sobre otra cuenta con ROLE_ADMIN.
     */
    @Transactional
    fun resetPassword(adminId: Long, email: String): AdminResetPasswordResponse {
        val user = userRepository.findByEmail(email.trim())
            ?: throw ResourceNotFoundException("No hay ninguna cuenta con ese correo")
        if (user.id == adminId) {
            throw BadRequestException("Para tu propia cuenta usá Perfil → Cambiar contraseña")
        }
        if (user.roles.any { it.name == "ROLE_ADMIN" }) {
            throw BadRequestException("No se puede restablecer la contraseña de otra cuenta de administrador")
        }
        val temporary = generateTemporaryPassword()
        user.passwordHash = passwordEncoder.encode(temporary) ?: ""
        userRepository.save(user)
        return AdminResetPasswordResponse(email = user.email, temporaryPassword = temporary)
    }

    // 10 caracteres sin los que se confunden al dictarlos (0/O, 1/l/I).
    private fun generateTemporaryPassword(length: Int = 10): String =
        (1..length).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")

    private companion object {
        val ROLE_NAMES = mapOf("Student" to "ROLE_STUDENT", "Teacher" to "ROLE_TEACHER", "Admin" to "ROLE_ADMIN")
        const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789"
    }
}
