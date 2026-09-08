package cr.ac.una.glifo.domain.repository

import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.AdminRoleInfo
import cr.ac.una.glifo.domain.model.AdminUserRow

interface AdminRepository {
    suspend fun getUsers(): Result<List<AdminUserRow>>
    suspend fun findUser(name: String): Result<AdminUserRow>
    suspend fun getRecentSignups(): Result<List<AdminUserRow>>
    suspend fun addUser(name: String, email: String, role: String): Result<Unit>
    suspend fun setActive(name: String, active: Boolean): Result<Unit>
    suspend fun getRoles(): Result<List<AdminRoleInfo>>
    suspend fun getPrivilegesFor(roleLabel: String): Result<List<String>>
}
