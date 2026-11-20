package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.data.datasource.remote.ApiErrorHandler
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.AdminChangeRoleRequest
import cr.ac.una.glifo.data.dto.AdminResetPasswordRequest
import cr.ac.una.glifo.data.dto.UserDto
import cr.ac.una.glifo.domain.model.AdminRole
import cr.ac.una.glifo.domain.model.AdminUser
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ApiHealth
import cr.ac.una.glifo.domain.repository.AdminRepository
import javax.inject.Inject

class AdminRepositoryImpl @Inject constructor(
    private val api: GlifoApi
) : AdminRepository {
    override suspend fun checkHealth(): Result<ApiHealth> =
        ApiErrorHandler.safeApiCall(
            apiCall = { api.getHealth() },
            transform = { response ->
                ApiHealth(
                    status = response.data.status,
                    service = response.data.service,
                    timestamp = response.data.timestamp
                )
            }
        )

    override suspend fun resetUserPassword(email: String): Result<String> =
        ApiErrorHandler.safeApiCall(
            apiCall = { api.adminResetPassword(AdminResetPasswordRequest(email.trim())) },
            transform = { it.data.temporaryPassword }
        )

    override suspend fun getUsers(): Result<List<AdminUser>> =
        ApiErrorHandler.safeApiCall(apiCall = { api.getAdminUsers() }, transform = { it.data.map { dto -> dto.toAdminUser() } })

    override suspend fun getRoles(): Result<List<AdminRole>> =
        ApiErrorHandler.safeApiCall(
            apiCall = { api.getAdminRoles() },
            transform = { it.data.map { dto -> AdminRole(dto.name, dto.description, dto.privileges, dto.userCount) } }
        )

    override suspend fun changeUserRole(userId: Long, role: GlifoRole): Result<AdminUser> =
        ApiErrorHandler.safeApiCall(
            apiCall = { api.changeUserRole(userId, AdminChangeRoleRequest(role.name)) },
            transform = { it.data.toAdminUser() }
        )

    private fun UserDto.toAdminUser() = AdminUser(
        id = id,
        email = email,
        fullName = listOfNotNull(firstName, lastName).joinToString(" ").trim().ifBlank { email },
        role = GlifoRole.entries.firstOrNull { it.name == role } ?: GlifoRole.Student
    )
}
