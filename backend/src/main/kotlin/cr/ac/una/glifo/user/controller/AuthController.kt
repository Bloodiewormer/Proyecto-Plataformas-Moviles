package cr.ac.una.glifo.user.controller

import cr.ac.una.glifo.common.dto.ApiResponse
import cr.ac.una.glifo.user.dto.AuthResponse
import cr.ac.una.glifo.user.dto.LoginRequest
import cr.ac.una.glifo.user.dto.RegisterRequest
import cr.ac.una.glifo.user.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val authService: AuthService) {
    
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse(data = response))
    }
    
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.login(request)
        return ResponseEntity.ok(ApiResponse(data = response))
    }
}
