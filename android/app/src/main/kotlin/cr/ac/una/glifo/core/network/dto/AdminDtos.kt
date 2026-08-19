package cr.ac.una.glifo.core.network.dto

data class UserAdminDto(
    val id: Long,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val isActive: Boolean,
    val roles: List<String>
)

data class UpdateRolesRequest(
    val roles: List<String>
)

data class UpdateStatusRequest(
    val isActive: Boolean
)
