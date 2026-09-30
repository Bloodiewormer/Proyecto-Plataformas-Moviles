package cr.ac.una.glifo.user.controller

import cr.ac.una.glifo.user.dto.AuthResponse
import cr.ac.una.glifo.user.dto.LoginRequest
import cr.ac.una.glifo.user.dto.RegisterRequest
import cr.ac.una.glifo.user.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * A diferencia del resto de la API, login y registro responden `{token, user}` sin el
 * envoltorio `data`: así lo consume la app (GlifoApi.login/register devuelven AuthResponse).
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request))

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.login(request))
}
