package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.data.datasource.remote.ApiErrorHandler
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.AdminResetPasswordRequest
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
}
