package cr.ac.una.glifo.data.repository

import cr.ac.una.glifo.core.common.AppError
import cr.ac.una.glifo.core.datastore.TokenManager
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import cr.ac.una.glifo.data.dto.AuthResponse
import cr.ac.una.glifo.data.dto.UserDto
import cr.ac.una.glifo.domain.error.Result
import cr.ac.una.glifo.domain.model.GlifoRole
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException

/**
 * Covers the error-mapping table in [AuthRepositoryImpl.callApi]'s KDoc — the piece of Lab 3's
 * "error handling" rubric line that previously had only manual Mockoon triggers (README) and
 * no automated evidence.
 */
class AuthRepositoryImplTest {

    private val api = mockk<GlifoApi>()
    private val tokenManager = mockk<TokenManager>()
    private val localUserDataCleaner = mockk<LocalUserDataCleaner>(relaxed = true)
    private val repository = AuthRepositoryImpl(api, tokenManager, devicePushManager = null, localUserDataCleaner = localUserDataCleaner)

    @Before
    fun setUp() {
        coEvery { tokenManager.getUserId() } returns null
        coEvery { tokenManager.clearAll() } returns Unit
        coEvery { tokenManager.saveToken(any(), any(), any()) } returns Unit
    }

    private fun httpException(code: Int, body: String = ""): HttpException {
        val responseBody = body.toResponseBody("application/json".toMediaTypeOrNull())
        return HttpException(Response.error<Any>(code, responseBody))
    }

    @Test
    fun `should return success and persist the token when login succeeds`() = runTest {
        val response = AuthResponse(
            token = "jwt-123",
            user = UserDto(id = 1L, email = "est@una.ac.cr", firstName = "Ana", lastName = "Pérez", role = "Student")
        )
        coEvery { api.login(any()) } returns response

        val result = repository.login("est@una.ac.cr", "password")

        assertTrue(result is Result.Success)
        assertEquals("est@una.ac.cr", (result as Result.Success).value.email)
        coVerify { tokenManager.saveToken(token = "jwt-123", userId = 1L, userEmail = "est@una.ac.cr") }
    }

    @Test
    fun `should map IOException to NoConnection`() = runTest {
        coEvery { api.login(any()) } throws IOException("mock not running")

        val result = repository.login("est@una.ac.cr", "password")

        assertEquals(Result.Failure(AppError.NoConnection), result)
    }

    @Test
    fun `should map timeout and connection reset to NoConnection`() = runTest {
        coEvery { api.login(any()) } throws SocketTimeoutException("timeout")
        assertEquals(Result.Failure(AppError.NoConnection), repository.login("est@una.ac.cr", "password"))

        coEvery { api.login(any()) } throws SocketException("Connection reset")
        assertEquals(Result.Failure(AppError.NoConnection), repository.login("est@una.ac.cr", "password"))
    }

    @Test
    fun `should map HTTP 401 to Unauthorized`() = runTest {
        coEvery { api.login(any()) } throws httpException(401)

        val result = repository.login("est@una.ac.cr", "wrong-password")

        assertEquals(Result.Failure(AppError.Unauthorized), result)
    }

    @Test
    fun `should map HTTP 400 to Validation with the mock's error body as the message`() = runTest {
        coEvery { api.login(any()) } throws httpException(400, "Credenciales inválidas")

        val result = repository.login("est@una.ac.cr", "password")

        assertEquals(
            Result.Failure(AppError.Validation("credentials", "Credenciales inválidas")),
            result
        )
    }

    @Test
    fun `should map HTTP 422 to Validation with a default message when the body is empty`() = runTest {
        coEvery { api.login(any()) } throws httpException(422, "")

        val result = repository.login("est@una.ac.cr", "password")

        assertEquals(
            Result.Failure(AppError.Validation("credentials", "Correo o contraseña incorrectos")),
            result
        )
    }

    @Test
    fun `should map any other HTTP status to Server with the code and a default message`() = runTest {
        coEvery { api.login(any()) } throws httpException(500, "")

        val result = repository.login("est@una.ac.cr", "password")

        assertEquals(
            Result.Failure(AppError.Server("500", "Error del servidor")),
            result
        )
    }

    @Test
    fun `should map an unexpected exception to Unknown`() = runTest {
        coEvery { api.login(any()) } throws IllegalStateException("boom")

        val result = repository.login("est@una.ac.cr", "password")

        assertEquals(Result.Failure(AppError.Unknown), result)
    }

    @Test
    fun `should never persist a token when login fails`() = runTest {
        coEvery { api.login(any()) } throws httpException(401)

        repository.login("est@una.ac.cr", "wrong-password")

        coVerify(exactly = 0) { tokenManager.saveToken(any(), any(), any()) }
    }

    @Test
    fun `should apply the same error mapping to register`() = runTest {
        coEvery { api.register(any()) } throws httpException(409, "El correo ya existe")

        val result = repository.register("Ana Pérez", "est@una.ac.cr", "password")

        assertEquals(
            Result.Failure(AppError.Server("409", "El correo ya existe")),
            result
        )
    }

    @Test
    fun `should return student, teacher and admin demo hints in debug builds`() = runTest {
        val hints = demoAccountHintsForBuild(isDebug = true)

        assertEquals(
            listOf(GlifoRole.Student, GlifoRole.Teacher, GlifoRole.Admin),
            hints.map { it.role }
        )
        assertEquals(Result.Success(hints), repository.getDemoAccountHints())
    }

    @Test
    fun `should exclude admin demo account in release builds`() {
        val hints = demoAccountHintsForBuild(isDebug = false)

        assertEquals(
            listOf(GlifoRole.Student, GlifoRole.Teacher),
            hints.map { it.role }
        )
        assertTrue(hints.none { it.role == GlifoRole.Admin })
    }

    @Test
    fun `should clear previous user local data and tokens when a different user logs in`() = runTest {
        coEvery { tokenManager.getUserId() } returns 1L
        coEvery { api.login(any()) } returns AuthResponse(
            token = "jwt-user-2",
            user = UserDto(id = 2L, email = "other@una.ac.cr", firstName = "Luis", lastName = "Mora", role = "Student")
        )

        val result = repository.login("other@una.ac.cr", "password")

        assertTrue(result is Result.Success)
        io.mockk.coVerifyOrder {
            tokenManager.clearAll()
            localUserDataCleaner.clearUserData()
            tokenManager.saveToken(token = "jwt-user-2", userId = 2L, userEmail = "other@una.ac.cr")
        }
    }

    @Test
    fun `should not clear local user data when the same user logs in again`() = runTest {
        coEvery { tokenManager.getUserId() } returns 1L
        coEvery { api.login(any()) } returns AuthResponse(
            token = "jwt-user-1",
            user = UserDto(id = 1L, email = "est@una.ac.cr", firstName = "Ana", lastName = "Pérez", role = "Student")
        )

        val result = repository.login("est@una.ac.cr", "password")

        assertTrue(result is Result.Success)
        coVerify(exactly = 0) { tokenManager.clearAll() }
        coVerify(exactly = 0) { localUserDataCleaner.clearUserData() }
        coVerify(exactly = 1) { tokenManager.saveToken(token = "jwt-user-1", userId = 1L, userEmail = "est@una.ac.cr") }
    }
}
