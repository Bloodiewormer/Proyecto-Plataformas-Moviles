package cr.ac.una.glifo.user.service

import cr.ac.una.glifo.common.exception.BadRequestException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.user.dto.AdminResetPasswordResponse
import cr.ac.una.glifo.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

/**
 * Acciones del admin sobre cuentas de otros usuarios. Hoy solo restablecer la contraseña:
 * es el camino de "Olvidé mi contraseña" mientras no haya envío de correos (Render en plan
 * gratis bloquea SMTP).
 */
@Service
class AdminUserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    private val random = SecureRandom()

    /**
     * Genera una contraseña temporal, la guarda cifrada y la devuelve en claro una sola vez.
     * El admin no puede usarlo sobre su propia cuenta: para eso está change-password, que pide la actual.
     */
    @Transactional
    fun resetPassword(adminId: Long, email: String): AdminResetPasswordResponse {
        val user = userRepository.findByEmail(email.trim())
            ?: throw ResourceNotFoundException("No hay ninguna cuenta con ese correo")
        if (user.id == adminId) {
            throw BadRequestException("Para tu propia cuenta usá Perfil → Cambiar contraseña")
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
        const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789"
    }
}
