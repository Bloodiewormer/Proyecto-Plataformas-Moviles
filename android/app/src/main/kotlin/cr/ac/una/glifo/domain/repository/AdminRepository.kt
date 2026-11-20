package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.ApiHealth
import cr.ac.una.glifo.domain.model.AdminRole
import cr.ac.una.glifo.domain.model.AdminUser
import cr.ac.una.glifo.domain.model.GlifoRole

interface AdminRepository {
    suspend fun checkHealth(): Result<ApiHealth>
    suspend fun resetUserPassword(email: String): Result<String>
    suspend fun getUsers(): Result<List<AdminUser>>
    suspend fun getRoles(): Result<List<AdminRole>>
    suspend fun changeUserRole(userId: Long, role: GlifoRole): Result<AdminUser>
}
