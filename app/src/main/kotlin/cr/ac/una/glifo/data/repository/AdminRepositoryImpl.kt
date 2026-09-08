package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.data.datasource.dummy.DummyAdminProvider
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.AdminRoleInfo
import cr.ac.una.glifo.domain.model.AdminUserRow
import cr.ac.una.glifo.domain.repository.AdminRepository
import javax.inject.Inject

class AdminRepositoryImpl @Inject constructor() : AdminRepository {
    override suspend fun getUsers(): Result<List<AdminUserRow>> =
        Result.Success(DummyAdminProvider.getUsers())

    override suspend fun findUser(name: String): Result<AdminUserRow> =
        Result.Success(DummyAdminProvider.findUser(name))

    override suspend fun getRecentSignups(): Result<List<AdminUserRow>> =
        Result.Success(DummyAdminProvider.getRecentSignups())

    override suspend fun addUser(name: String, email: String, role: String): Result<Unit> {
        DummyAdminProvider.addUser(name, email, role)
        return Result.Success(Unit)
    }

    override suspend fun setActive(name: String, active: Boolean): Result<Unit> {
        DummyAdminProvider.setActive(name, active)
        return Result.Success(Unit)
    }

    override suspend fun getRoles(): Result<List<AdminRoleInfo>> =
        Result.Success(DummyAdminProvider.getRoles())

    override suspend fun getPrivilegesFor(roleLabel: String): Result<List<String>> =
        Result.Success(DummyAdminProvider.getPrivilegesFor(roleLabel))
}
