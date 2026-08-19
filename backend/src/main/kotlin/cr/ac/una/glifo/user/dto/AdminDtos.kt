package cr.ac.una.glifo.user.dto

import jakarta.validation.constraints.NotEmpty

data class UserAdminResponse(
    val id: Long,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val isActive: Boolean,
    val roles: List<String>
)

data class UpdateRolesRequest(
    @field:NotEmpty val roles: List<String>
)

data class UpdateStatusRequest(
    val isActive: Boolean
)
