package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ApiHealth

interface AdminRepository {
    suspend fun checkHealth(): Result<ApiHealth>
    suspend fun resetUserPassword(email: String): Result<String>
}
