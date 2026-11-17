package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.datastore.TokenManager
import cr.ac.una.glifo.core.push.DevicePushManager
import cr.ac.una.glifo.data.datasource.dummy.DummyAuthProvider
import cr.ac.una.glifo.data.datasource.remote.ApiErrorHandler
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.AuthResponse
import cr.ac.una.glifo.data.dto.ChangePasswordRequest
import cr.ac.una.glifo.data.dto.LoginRequest
import cr.ac.una.glifo.data.dto.RegisterRequest
import cr.ac.una.glifo.data.dto.UserDto
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.DemoAccountHint
import cr.ac.una.glifo.domain.model.GlifoRole
import cr.ac.una.glifo.domain.model.User
import cr.ac.una.glifo.domain.repository.AuthRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Login/register go through Retrofit + Gson (`GlifoApi`) against a mock API — the Lab 3
 * requirement (`Contexto_Curso.md`, "Error handling, Hilt, Retrofit, mock API"). The mock is
 * built with Mockoon per the professor's own lesson on the topic (see
 * `Glifo_Bitacora_Decisiones.md` for the tool choice and the route contract); point
 * `NetworkModule`'s base URL at wherever Mockoon is serving.
 *
 * `getDemoAccountHints` stays on [DummyAuthProvider] — they're dummy-phase-only
 * UI affordances (see [DemoAccountHint]'s KDoc) with no backend endpoint to call, not part
 * of what Lab 3 evaluates. Everything else in the app (courses, notes, study, …) is still
 * 100% dummy-backed per the standing "temporary until there's a real backend" decision —
 * this repository is the one deliberate exception, scoped to what Lab 3 grades.
 */
class AuthRepositoryImpl @Inject constructor(
    private val api: GlifoApi,
    private val tokenManager: TokenManager,
    private val devicePushManager: DevicePushManager?
) : AuthRepository {

    constructor(
        api: GlifoApi,
        tokenManager: TokenManager
    ) : this(
        api = api,
        tokenManager = tokenManager,
        devicePushManager = null
    )

    override suspend fun login(email: String, password: String): Result<User> =
        callApi { api.login(LoginRequest(email = email.trim(), password = password)) }

    override suspend fun register(fullName: String, email: String, password: String): Result<User> {
        val (firstName, lastName) = fullName.trim().splitName()
        val request = RegisterRequest(
            email = email.trim(),
            password = password,
            firstName = firstName,
            lastName = lastName
        )
        return callApi { api.register(request) }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
        ApiErrorHandler.safeApiCall(
            apiCall = {
                val response = api.changePassword(ChangePasswordRequest(currentPassword, newPassword))
                // Response<Unit> no lanza en 4xx: se convierte a HttpException para mapear el error igual que el resto.
                if (!response.isSuccessful) throw HttpException(response)
            },
            transform = {}
        )

    override suspend fun getDemoAccountHints(): Result<List<DemoAccountHint>> =
        Result.Success(DummyAuthProvider.demoAccounts.map { DemoAccountHint(it.email, it.password, it.role) })

    /**
     * Runs one auth call and turns its outcome into [Result], per the error-handling
     * pattern in Glifo_Arquitectura_Estandares.md §9: no exception escapes the data layer.
     *  - No network / mock not running        → [AppError.NoConnection]
     *  - 401 from the mock                     → [AppError.Unauthorized]
     *  - 400/422 (bad credentials, bad payload) → [AppError.Validation]
     *  - Any other HTTP status                 → [AppError.Server]
     *  - Anything unexpected                   → [AppError.Unknown]
     * On success, the JWT + user id/email are persisted via [TokenManager] so
     * `AuthInterceptor` can attach it to every later call, and the device + FCM token
     * are registered/reconciled for push targeting (#48).
     */
    private suspend fun callApi(block: suspend () -> AuthResponse): Result<User> {
        return try {
            val response = block()
            tokenManager.saveToken(
                token = response.token,
                userId = response.user.id,
                userEmail = response.user.email
            )
            devicePushManager?.reconcileDeviceOnSessionStart()
            Result.Success(response.user.toDomain())
        } catch (e: IOException) {
            Result.Failure(AppError.NoConnection)
        } catch (e: HttpException) {
            Result.Failure(e.toAppError())
        } catch (e: Exception) {
            Result.Failure(AppError.Unknown)
        }
    }

    private fun HttpException.toAppError(): AppError = when (code()) {
        401 -> AppError.Unauthorized
        400, 422 -> AppError.Validation("credentials", parseServerMessage() ?: "Correo o contraseña incorrectos")
        else -> AppError.Server(code().toString(), parseServerMessage() ?: "Error del servidor")
    }

    /** Best-effort read of the mock's error body; the mock isn't required to follow §8.3's envelope. */
    private fun HttpException.parseServerMessage(): String? =
        try {
            response()?.errorBody()?.string()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
}

private fun UserDto.toDomain(): User = User(
    id = id,
    fullName = listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { email.substringBefore("@") },
    email = email,
    role = role.toGlifoRole()
)

private fun String.toGlifoRole(): GlifoRole =
    GlifoRole.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: GlifoRole.Student

/** "Brandon Brenes" -> ("Brandon", "Brenes"); "Brandon" -> ("Brandon", ""). */
private fun String.splitName(): Pair<String, String> {
    val parts = trim().split(" ", limit = 2)
    return parts[0] to (parts.getOrNull(1) ?: "")
}
