package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.data.datasource.dummy.DummyAccount
import cr.ac.una.glifo.data.datasource.dummy.DummyAuthProvider
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.DemoAccountHint
import cr.ac.una.glifo.domain.model.User
import cr.ac.una.glifo.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> {
        val account = DummyAuthProvider.findAccount(email, password)
            ?: return Result.Failure(AppError.Validation("password", "Correo o contraseña incorrectos"))
        return Result.Success(account.toDomain())
    }

    override suspend fun register(fullName: String, email: String, password: String): Result<User> {
        if (DummyAuthProvider.emailExists(email)) {
            return Result.Failure(AppError.Validation("email", "Ya existe una cuenta con ese correo"))
        }
        val account = DummyAuthProvider.register(fullName, email, password)
        return Result.Success(account.toDomain())
    }

    override suspend fun emailExists(email: String): Result<Boolean> =
        Result.Success(DummyAuthProvider.emailExists(email))

    override suspend fun getDemoAccountHints(): Result<List<DemoAccountHint>> =
        Result.Success(DummyAuthProvider.demoAccounts.map { DemoAccountHint(it.email, it.password, it.role) })
}

private fun DummyAccount.toDomain(): User =
    User(id = id, fullName = fullName, email = email, role = role)
