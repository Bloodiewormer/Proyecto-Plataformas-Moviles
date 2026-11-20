package cr.ac.una.glifo.user.controller

import cr.ac.una.glifo.security.GlifoUserDetails
import cr.ac.una.glifo.user.dto.AuthResponse
import cr.ac.una.glifo.user.dto.ChangePasswordRequest
import cr.ac.una.glifo.user.dto.ForgotPasswordRequest
import cr.ac.una.glifo.user.dto.LoginRequest
import cr.ac.una.glifo.user.dto.RegisterRequest
import cr.ac.una.glifo.user.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * A diferencia del resto de la API, login y registro responden `{token, user}` sin el
 * envoltorio `data`: así lo consume la app (GlifoApi.login/register devuelven AuthResponse).
 *
 * | Método | Ruta           | Éxito | Errores                            |
 * |--------|----------------|-------|------------------------------------|
 * | POST   | /auth/register | 201   | 400, 409 (correo ya registrado), 415 |
 * | POST   | /auth/login    | 200   | 400, 401 (credenciales), 415       |
 * | POST   | /auth/change-password | 204 | 400 (actual incorrecta, igual a la actual o inválida), 401 (sin token), 415 |
 * | POST   | /auth/forgot-password | 204 | 400 (inválida), 404 (correo no registrado), 415 |
 *
 * change-password es la única de estas rutas que pide token (no está en permitAll).
 */
@RestController
@RequestMapping(
    "\${url.auth}",
    produces = [MediaType.APPLICATION_JSON_VALUE],
    consumes = [MediaType.APPLICATION_JSON_VALUE]
)
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request))

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.login(request))

    @PostMapping("/change-password")
    fun changePassword(
        @AuthenticationPrincipal user: GlifoUserDetails,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<Void> {
        authService.changePassword(user.userId, request)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<Void> {
        authService.forgotPassword(request)
        return ResponseEntity.noContent().build()
    }
}
