package cr.ac.una.glifo.core.network.dto

data class LoginRequest(
    val email: String, 
    val password: String
)

data class RegisterRequest(
    val email: String, 
    val password: String, 
    val firstName: String? = null, 
    val lastName: String? = null
)

data class AuthResponse(
    val token: String, 
    val user: UserDto
)

data class UserDto(
    val id: Long, 
    val email: String, 
    val firstName: String? = null, 
    val lastName: String? = null, 
    val roles: List<String> = emptyList()
)
