package cr.ac.una.glifo.user.service

import cr.ac.una.glifo.common.exception.BadRequestException
import cr.ac.una.glifo.common.exception.DuplicateResourceException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.security.JwtProvider
import cr.ac.una.glifo.user.dto.AuthResponse
import cr.ac.una.glifo.user.dto.ChangePasswordRequest
import cr.ac.una.glifo.user.dto.ForgotPasswordRequest
import cr.ac.una.glifo.user.dto.LoginRequest
import cr.ac.una.glifo.user.dto.RegisterRequest
import cr.ac.una.glifo.user.entity.User
import cr.ac.una.glifo.user.mapper.toResponse
import cr.ac.una.glifo.user.repository.RoleRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager,
    private val jwtProvider: JwtProvider,
) {

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw DuplicateResourceException("Ya existe una cuenta con ese correo")
        }

        val user = User()
        user.email = request.email
        user.passwordHash = passwordEncoder.encode(request.password) ?: ""
        user.firstName = request.firstName
        user.lastName = request.lastName

        roleRepository.findByName("ROLE_STUDENT")?.let {
            user.roles.add(it)
        }

        val savedUser = userRepository.save(user)
        val token = jwtProvider.generateToken(
            savedUser.email, 
            savedUser.id, 
            savedUser.roles.map { it.name },
        )
        
        return AuthResponse(token, savedUser.toResponse())
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthResponse {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )
        
        val user = userRepository.findByEmail(request.email) 
            ?: throw RuntimeException("User not found")
            
        val token = jwtProvider.generateToken(authentication)
        
        return AuthResponse(token, user.toResponse())
    }

    /**
     * Cambia la contraseña del usuario autenticado. Exige la actual para que un teléfono
     * desbloqueado con la sesión abierta no baste para cambiarla.
     * Responde 400 (no 401) si la actual no coincide: la app trata un 401 como sesión vencida.
     */
    @Transactional
    fun changePassword(userId: Long, request: ChangePasswordRequest) {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("Usuario no encontrado") }
        if (!passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
            throw BadRequestException("La contraseña actual no es correcta")
        }
        if (passwordEncoder.matches(request.newPassword, user.passwordHash)) {
            throw BadRequestException("La contraseña nueva debe ser distinta de la actual")
        }
        user.passwordHash = passwordEncoder.encode(request.newPassword) ?: ""
        userRepository.save(user)
    }

    /** Cambia la contraseña sin sesión, solo con el correo (ver [ForgotPasswordRequest]). */
    @Transactional
    fun forgotPassword(request: ForgotPasswordRequest) {
        val user = userRepository.findByEmail(request.email.trim())
            ?: throw ResourceNotFoundException("No existe una cuenta con ese correo")
        user.passwordHash = passwordEncoder.encode(request.newPassword) ?: ""
        userRepository.save(user)
    }
}
